package org.icpclive.cds.yandex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestStatus
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.YandexSettings
import org.icpclive.cds.yandex.api.*
import org.icpclive.util.getLogger
import org.icpclive.util.loopFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class YandexDataSource(settings: YandexSettings) : ContestDataSource {
    private val apiKey = settings.apiKey.value
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


        contestDescriptionLoader = jsonUrlLoader(settings.network, auth) { "$API_BASE/contests/${settings.contestId}" }
        problemLoader = jsonUrlLoader<Problems>(settings.network, auth) { "$API_BASE/contests/${settings.contestId}/problems" }.map {
            it.problems.sortedBy { it.alias }
        }
        participantLoader = run {
            val participantRegex = settings.loginRegex
            jsonUrlLoader<List<Participant>>(settings.network, auth) { "$API_BASE/contests/${settings.contestId}/participants" }.map {
                it.filter { participant -> participant.login.matches(participantRegex) }
            }
        }
        allSubmissionsLoader = jsonUrlLoader<Submissions>(settings.network, auth) {
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
            val info = rawContestInfoFlow.value.toApi()
            if (info.status == ContestStatus.OVER) {
                emit(InfoUpdate(info.copy(status = ContestStatus.RUNNING)))
            } else {
                emit(InfoUpdate(info))
            }
            val allSubmissions = allSubmissionsLoader.load()
            with (rawContestInfoFlow.value) {
                emitAll(
                    allSubmissions.sortedWith(compareBy({ it.time }, { it.id })).filter(this::isTeamSubmission)
                        .map { RunUpdate(submissionToRun(it)) }
                        .asFlow()
                )
            }
            if (info.status == ContestStatus.OVER) {
                emit(InfoUpdate(info))
            }
            val newSubmissionsFlow = newSubmissionsFlow(1.seconds)
            val allSubmissionsFlow = loopFlow(
                120.seconds,
                onError = { getLogger(YandexDataSource::class).error("Failed to reload data, retrying", it) }
            ) { allSubmissionsLoader.load() }
                .onStart { delay(120.seconds) }
                .flowOn(Dispatchers.IO)
            val allRunsFlow = merge(allSubmissionsFlow, newSubmissionsFlow).map {
                with(rawContestInfoFlow.value) {
                    it.sortedWith(compareBy({it.time}, { it.id })).filter(this::isTeamSubmission).map { RunUpdate(submissionToRun(it)) }
                }
            }.flatMapConcat { it.asFlow() }
            emitAll(merge(allRunsFlow, rawContestInfoFlow.map { InfoUpdate(it.toApi()) }))
        }
    }

    companion object {
        private val log = getLogger(YandexDataSource::class)
        const val API_BASE = "https://api.contest.yandex.net/api/public/v2"
    }

    private fun newSubmissionsFlow(
        period: Duration
    ) : Flow<List<Submission>> {
        log.info("HERE!!!")
        val formatter = Json { ignoreUnknownKeys = true }
        var pendingRunId = 0L

        return loopFlow(
            period,
            { log.error("Fail to load new submissions", it) }
        ) {
            buildList {
                var page = 1
                while (true) {
                    log.info("Plan to load: submissions?locale=ru&page=$page&pageSize=100")
                    val response = httpClient.request("submissions?locale=ru&page=$page&pageSize=100") {}
                    log.info("Loaded")
                    val pageSubmissions = formatter.decodeFromString<Submissions>(response.body()).submissions
                    log.info(pageSubmissions.toString())
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
