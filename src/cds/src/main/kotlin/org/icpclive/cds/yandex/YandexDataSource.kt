package org.icpclive.cds.yandex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestResultType
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.RawContestDataSource
import org.icpclive.cds.common.*
import org.icpclive.cds.yandex.YandexConstants.API_BASE
import org.icpclive.cds.yandex.YandexConstants.CONTEST_ID_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.LOGIN_PREFIX_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.TOKEN_PROPERTY_NAME
import org.icpclive.cds.yandex.api.*
import org.icpclive.util.*
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexDataSource(props: Properties, creds: Map<String, String>) : RawContestDataSource {
    private val apiKey: String
    private val loginPrefix: String
    private val contestId: Long
    private val httpClient: HttpClient

    private val contestDescriptionLoader: DataLoader<ContestDescription>
    private val problemLoader: DataLoader<List<Problem>>
    private val participantLoader: DataLoader<List<Participant>>
    private val allSubmissionsLoader: DataLoader<List<Submission>>

    val resultType = ContestResultType.valueOf(props.getProperty("standings.resultType", "ICPC").uppercase())


    init {
        apiKey = props.getCredentials(TOKEN_PROPERTY_NAME, creds) ?: throw IllegalStateException("YC api key is not defined")
        contestId = props.getProperty(CONTEST_ID_PROPERTY_NAME).toLong()
        loginPrefix = props.getProperty(LOGIN_PREFIX_PROPERTY_NAME)

        val auth = ClientAuth.OAuth(apiKey)
        httpClient = defaultHttpClient(auth) {
            defaultRequest {
                url("$API_BASE/contests/$contestId/")
            }
            engine {
                requestTimeout = 40000
            }
        }


        contestDescriptionLoader = jsonLoader(auth) { "$API_BASE/contests/$contestId" }
        problemLoader = jsonLoader<Problems>(auth) { "$API_BASE/contests/$contestId/problems" }.map {
            it.problems.sortedBy { it.alias }
        }
        participantLoader = run {
            val participantRegex = Regex(loginPrefix)
            jsonLoader<List<Participant>>(auth) { "$API_BASE/contests/$contestId/participants" }.map {
                it.filter { participant -> participant.login.matches(participantRegex) }
            }
        }
        allSubmissionsLoader = jsonLoader<Submissions>(auth) {
            "$API_BASE/contests/$contestId/submissions?locale=ru&page=1&pageSize=100000"
        }.map { it.submissions.reversed() }
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
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
            analyticsMessagesDeferred.complete(emptyFlow())
            contestInfoDeferred.complete(rawContestInfoFlow.map { it.toApi()}.stateIn(this))
            val newSubmissionsFlow = newSubmissionsFlow(1.seconds)
            val allSubmissionsFlow = allSubmissionsLoader.reloadFlow(120.seconds).flowOn(Dispatchers.IO)
            val submissionsFlow = merge(allSubmissionsFlow, newSubmissionsFlow).map {
                with(rawContestInfoFlow.value) {
                    it.mapNotNull { submission ->
                        if (isTeamSubmission(submission))
                            submissionToRun(submission)
                        else
                            null
                    }
                }
            }
            launch { RunsBufferService(submissionsFlow, runsDeferred).run() }
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        val rawContestInfo = YandexContestInfo(
            contestDescriptionLoader.load(),
            problemLoader.load(),
            participantLoader.load(),
            resultType
        )
        val contestInfo = rawContestInfo.toApi()

        log.info("Loading all contest submissions")
        val submissions = allSubmissionsLoader.load()
            .filter(rawContestInfo::isTeamSubmission)
            .map(rawContestInfo::submissionToRun)
        log.info("Loaded all submissions for emulation")
        return ContestParseResult(contestInfo, submissions, emptyList())
    }

    companion object {
        private val log = getLogger(YandexDataSource::class)
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
