package org.icpclive.cds.yandex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.yandex.YandexConstants.API_BASE
import org.icpclive.cds.yandex.YandexConstants.CONTEST_ID_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.LOGIN_PREFIX_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.TOKEN_PROPERTY_NAME
import org.icpclive.cds.yandex.api.*
import org.icpclive.cds.common.RegularLoaderService
import org.icpclive.cds.common.RunsBufferService
import org.icpclive.cds.common.defaultHttpClient
import org.icpclive.common.util.awaitSubscribers
import org.icpclive.common.util.getCredentials
import org.icpclive.common.util.getLogger
import java.io.IOException
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexDataSource(props: Properties, creds: Map<String, String>) : ContestDataSource {
    private val apiKey: String
    private val loginPrefix: String
    private val contestId: Long
    private val httpClient: HttpClient

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    private val contestDescriptionLoader: RegularLoaderService<ContestDescription>
    private val problemLoader: RegularLoaderService<List<Problem>>
    private val participantLoader: RegularLoaderService<List<Participant>>
    private val allSubmissionsLoader: RegularLoaderService<List<Submission>>


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


        contestDescriptionLoader = object : RegularLoaderService<ContestDescription>(auth) {
            override val url = "$API_BASE/contests/$contestId"
            override fun processLoaded(data: String) =
                formatter.decodeFromString<ContestDescription>(data)
        }

        problemLoader = object : RegularLoaderService<List<Problem>>(auth) {
            override val url = "$API_BASE/contests/$contestId/problems"
            override fun processLoaded(data: String) =
                formatter.decodeFromString<Problems>(data).problems.sortedBy { it.alias }
        }

        participantLoader = object : RegularLoaderService<List<Participant>>(auth) {
            override val url = "$API_BASE/contests/$contestId/participants"
            override fun processLoaded(data: String) =
                formatter.decodeFromString<List<Participant>>(data).filter { it.login.matches(Regex(loginPrefix)) }
        }

        allSubmissionsLoader = object : RegularLoaderService<List<Submission>>(auth) {
            override val url = "$API_BASE/contests/$contestId/submissions?locale=ru&page=1&pageSize=100000"
            override fun processLoaded(data: String) =
                formatter.decodeFromString<Submissions>(data).submissions.reversed()
        }
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        val rawContestInfo = YandexContestInfo(
            contestDescriptionLoader.loadOnce(),
            problemLoader.loadOnce(),
            participantLoader.loadOnce()
        )
        val contestInfo = rawContestInfo.toApi()

        val rawContestInfoFlow = MutableStateFlow(rawContestInfo)
        val contestInfoFlow = MutableStateFlow(contestInfo)

        val runsBufferFlow = MutableSharedFlow<List<RunInfo>>(
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        coroutineScope {
            analyticsMessagesDeferred.complete(emptyFlow())
            contestInfoDeferred.complete(contestInfoFlow)
            launch(Dispatchers.IO) { reloadContestInfo(rawContestInfoFlow, contestInfoFlow, 30.seconds) }
            launch(Dispatchers.IO) {
                runsBufferFlow.awaitSubscribers()
                fetchNewRunsOnly(rawContestInfoFlow, runsBufferFlow, 1.seconds)
            }
            launch(Dispatchers.IO) {
                runsBufferFlow.awaitSubscribers()
                reloadAllRuns(rawContestInfoFlow, runsBufferFlow, 120.seconds)
            }
            launch { RunsBufferService(runsBufferFlow, runsDeferred).run() }
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        val rawContestInfo = YandexContestInfo(
            contestDescriptionLoader.loadOnce(),
            problemLoader.loadOnce(),
            participantLoader.loadOnce()
        )
        val contestInfo = rawContestInfo.toApi()

        log.info("Loading all contest submissions")
        val submissions = allSubmissionsLoader.loadOnce()
            .filter(rawContestInfo::isTeamSubmission)
            .map(rawContestInfo::submissionToRun)
        log.info("Loaded all submissions for emulation")
        return ContestParseResult(contestInfo, submissions, emptyList())
    }

    companion object {
        private val log = getLogger(YandexDataSource::class)
    }

    // TODO: try .stateIn
    private suspend fun reloadContestInfo(
        rawFlow: MutableStateFlow<YandexContestInfo>,
        flow: MutableStateFlow<ContestInfo>,
        period: Duration
    ) {
        while (true) {
            try {
                val info = YandexContestInfo(
                    contestDescriptionLoader.loadOnce(),
                    problemLoader.loadOnce(),
                    participantLoader.loadOnce()
                )
                rawFlow.value = info
                flow.value = info.toApi()
            } catch (e: IOException) {
                log.error("Failed to reload ContestInfo", e)
            }
            delay(period)
        }
    }

    private suspend fun reloadAllRuns(
        rawContestInfoFlow: MutableStateFlow<YandexContestInfo>,
        runsBufferFlow: MutableSharedFlow<List<RunInfo>>,
        period: Duration
    ) {
        while (true) {
            try {
                val rawContestInfo = rawContestInfoFlow.value
                val submissions = allSubmissionsLoader.loadOnce()
                    .filter(rawContestInfo::isTeamSubmission)
                    .map(rawContestInfo::submissionToRun)
                runsBufferFlow.emit(submissions)
            } catch (e: IOException) {
                log.error("Failed to reload rejudges", e)
            }
            delay(period)
        }
    }

    private suspend fun fetchNewRunsOnly(
        rawContestInfoFlow: MutableStateFlow<YandexContestInfo>,
        runsBufferFlow: MutableSharedFlow<List<RunInfo>>,
        period: Duration
    ) {
        var pendingRunId = 0
        while (true) {
            try {
                val rawContestInfo = rawContestInfoFlow.value
                var page = 1
                val runs = mutableListOf<RunInfo>()
                while (true) {
                    val response = httpClient.request("submissions?locale=ru&page=$page&pageSize=100") {}
                    val pageSubmissions = formatter.decodeFromString<Submissions>(response.body()).submissions
                    runs.addAll(
                        pageSubmissions
                            .filter(rawContestInfo::isTeamSubmission)
                            .map(rawContestInfo::submissionToRun)
                    )
                    if (pageSubmissions.isEmpty() || pageSubmissions.last().id <= pendingRunId) {
                        break
                    }
                    page++
                }
                pendingRunId = runs.filter { !it.isJudged }.minOfOrNull { it.id } ?: runs.maxOfOrNull { it.id } ?: 0
                runsBufferFlow.emit(runs)
            } catch (e: IOException) {
                log.error("Failed to reload rejudges", e)
            }
            delay(period)
        }
    }
}
