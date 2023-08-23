package org.icpclive.cds.yandex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestStatus
import org.icpclive.cds.*
import org.icpclive.cds.adapters.withGroupedRuns
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.YandexSettings
import org.icpclive.cds.yandex.api.*
import org.icpclive.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class YandexDataSource(settings: YandexSettings, creds: Map<String, String>) : ContestDataSource {
    private val apiKey = settings.apiKey.get(creds)
    private val httpClient: HttpClient

    private val contestDescriptionLoader: DataLoader<ContestDescription>
    private val problemLoader: DataLoader<List<Problem>>
    private val participantLoader: DataLoader<List<Participant>>
    private val allSubmissionsLoader: DataLoader<List<Submission>>

    val resultType = settings.resultType


    init {
        val auth = ClientAuth.OAuth(apiKey)
        httpClient = defaultHttpClient(auth, settings.network) {
            defaultRequest {
                url("$API_BASE/contests/${settings.contestId}/")
            }
            engine {
                requestTimeout = 40000
            }
        }


        contestDescriptionLoader = jsonLoader(settings.network, auth) { "$API_BASE/contests/${settings.contestId}" }
        problemLoader = jsonLoader<Problems>(settings.network, auth) { "$API_BASE/contests/${settings.contestId}/problems" }.map {
            it.problems.sortedBy { it.alias }
        }
        participantLoader = run {
            val participantRegex = settings.loginRegex
            jsonLoader<List<Participant>>(settings.network, auth) { "$API_BASE/contests/${settings.contestId}/participants" }.map {
                it.filter { participant -> participant.login.matches(participantRegex) }
            }
        }
        allSubmissionsLoader = jsonLoader<Submissions>(settings.network, auth) {
            "$API_BASE/contests/${settings.contestId}/submissions?locale=ru&page=1&pageSize=100000"
        }.map { it.submissions.reversed() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFlow() = flow {
        coroutineScope {
            val rawContestInfoFlow = loopFlow(
                30.seconds,
                { log.error("Failed to reload contest info", it) }
            ) {
                YandexContestInfo(
                    contestDescriptionLoader.load(),
                    problemLoader.load(),
                    participantLoader.load(),
                    resultType
                )
            }.flowOn(Dispatchers.IO)
                .stateIn(this)
            emit(InfoUpdate(rawContestInfoFlow.value.toApi()))
            val newSubmissionsFlow = newSubmissionsFlow(1.seconds)
            val allSubmissionsFlow = loopFlow(
                120.seconds,
                onError = { getLogger(YandexDataSource::class).error("Failed to reload data, retrying", it) }
            ) { allSubmissionsLoader.load() }
                .flowOn(Dispatchers.IO)
            val allRunsFlow = merge(allSubmissionsFlow, newSubmissionsFlow).map {
                with(rawContestInfoFlow.value) {
                    it.sortedWith(compareBy({it.time}, { it.id })).filter(this::isTeamSubmission).map { RunUpdate(submissionToRun(it)) }
                }
            }.flatMapConcat { it.asFlow() }
            emitAll(merge(allRunsFlow, rawContestInfoFlow.map { InfoUpdate(it.toApi()) }))
        }
    }.withGroupedRuns({ it.result != null })
        .transformWhile {
            emit(it.event)
            if (it.infoAfterEvent?.status == ContestStatus.OVER  && it.runs[false].isNullOrEmpty()) {
                emit(InfoUpdate(it.infoAfterEvent!!.copy(status = ContestStatus.FINALIZED)))
                false
            } else {
                true
            }
        }

    companion object {
        private val log = getLogger(YandexDataSource::class)
        const val API_BASE = "https://api.contest.yandex.net/api/public/v2"
    }

    private suspend fun newSubmissionsFlow(
        period: Duration
    ) : Flow<List<Submission>> {
        val formatter = Json { ignoreUnknownKeys = true }
        var pendingRunId = 0L

        return loopFlow(
            period,
            { log.error("Fail to load new submissions", it) }
        ) {
            buildList {
                var page = 1
                while (true) {
                    val response = httpClient.request("submissions?locale=ru&page=$page&pageSize=100") {}
                    val pageSubmissions = formatter.decodeFromString<Submissions>(response.body()).submissions
                    addAll(pageSubmissions)
                    if (pageSubmissions.isEmpty() || pageSubmissions.last().id <= pendingRunId) {
                        break
                    }
                    page++
                }
            }.also { runs ->
                pendingRunId = runs.filter { it.verdict == "" }.minOfOrNull { it.id } ?: runs.maxOfOrNull { it.id } ?: 0
            }
        }
    }
}
