package org.icpclive.cds.plugins.yandex

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.plugins.yandex.api.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Builder("yandex")
public sealed interface YandexSettings : CDSSettings {
    public val apiKey: Credential
    public val loginRegex: Regex
    public val contestId: Int
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC

    override fun toDataSource(): ContestDataSource = YandexDataSource(this)
}

internal class YandexDataSource(private val settings: YandexSettings) : ContestDataSource {
    private val contestBaseUrl = API_BASE.subDir("contests").subDir(settings.contestId.toString())
        .withOAuth(settings.apiKey)

    private val contestDescriptionLoader = DataLoader.json<ContestDescription>(settings.network, contestBaseUrl)
    private val problemLoader = DataLoader.json<Problems>(settings.network, contestBaseUrl.subDir("problems")).map {
        it.problems.sortedBy { it.alias }
    }
    private val participantLoader = DataLoader.json<List<Participant>>(settings.network, contestBaseUrl.subDir("participants")) .map {
        it.filter { participant -> participant.login.matches(settings.loginRegex) }
    }
    private val allSubmissionsLoader = DataLoader.json<Submissions>(settings.network, contestBaseUrl.subDir("submissions?locale=ru&page=1&pageSize=100000")).map {
        it.submissions.reversed()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getFlow() = flow {
        coroutineScope {
            val rawContestInfoFlow = loopFlow(
                30.seconds,
                { log.error(it) { "Failed to reload contest info" } }
            ) {
                YandexContestInfo(
                    contestDescriptionLoader.load(),
                    problemLoader.load(),
                    participantLoader.load(),
                    settings.resultType
                )
            }.flowOn(Dispatchers.IO)
                .stateIn(this)
            val info = rawContestInfoFlow.value.toApi()
            val status = info.status
            if (status is ContestStatus.OVER) {
                emit(InfoUpdate(info.copy(status = ContestStatus.RUNNING(status.startedAt, status.frozenAt, isFake = true))))
            } else {
                emit(InfoUpdate(info))
            }
            val allSubmissions = allSubmissionsLoader.load()
            with(rawContestInfoFlow.value) {
                emitAll(
                    allSubmissions.sortedWith(compareBy({ it.time }, { it.id }))
                        .map { RunUpdate(submissionToRun(it)) }
                        .asFlow()
                )
            }
            if (info.status is ContestStatus.OVER) {
                emit(InfoUpdate(info))
            }
            val newSubmissionsFlow = newSubmissionsFlow(1.seconds)
            val allSubmissionsFlow = loopFlow(
                120.seconds,
                onError = { log.error(it) { "Failed to reload data, retrying" } }
            ) { allSubmissionsLoader.load() }
                .onStart { delay(120.seconds) }
                .flowOn(Dispatchers.IO)
            val allRunsFlow = merge(allSubmissionsFlow, newSubmissionsFlow).map {
                with(rawContestInfoFlow.value) {
                    it.sortedWith(compareBy({ it.time }, { it.id }))
                        .map { RunUpdate(submissionToRun(it)) }
                }
            }.flatMapConcat { it.asFlow() }
            emitAll(merge(allRunsFlow, rawContestInfoFlow.map { InfoUpdate(it.toApi()) }))
        }
    }

    companion object {
        private val log by getLogger()
        private val API_BASE = UrlOrLocalPath.Url("https://api.contest.yandex.net/api/public/v2")
    }

    private fun newSubmissionsFlow(
        period: Duration,
    ): Flow<List<Submission>> {
        val formatter = Json { ignoreUnknownKeys = true }
        var pendingRunId = 0L

        return loopFlow(
            period,
            { log.error(it) { "Fail to load new submissions" } }
        ) {
            buildList {
                var page = 1
                val loader = DataLoader.json<Submissions>(settings.network) {
                    contestBaseUrl.subDir("submissions?locale=ru&page=$page&pageSize=100")
                }.map { it.submissions }
                while (true) {
                    val pageSubmissions = loader.load()
                    log.debug{ pageSubmissions.toString() }
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
