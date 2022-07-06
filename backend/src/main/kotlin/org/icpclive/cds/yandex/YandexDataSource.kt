package org.icpclive.cds.yandex

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.yandex.YandexConstants.API_BASE
import org.icpclive.cds.yandex.YandexConstants.CONTEST_ID_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.LOGIN_PREFIX_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.TOKEN_PROPERTY_NAME
import org.icpclive.cds.yandex.api.*
import org.icpclive.config.Config
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.RunsBufferService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.OAuthAuth
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger
import org.icpclive.utils.processCreds
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexDataSource : ContestDataSource {
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
        val props = Config.loadProperties("events")
        apiKey = props.getProperty(TOKEN_PROPERTY_NAME).processCreds()
        contestId = props.getProperty(CONTEST_ID_PROPERTY_NAME).toLong()
        loginPrefix = props.getProperty(LOGIN_PREFIX_PROPERTY_NAME)

        httpClient = defaultHttpClient(OAuthAuth(apiKey)) {
            defaultRequest {
                url("$API_BASE/contests/$contestId/")
            }
            engine {
                requestTimeout = 40000
            }
        }

        val auth = OAuthAuth(apiKey)

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

    override suspend fun run() {
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
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val rawRunsFlow = MutableSharedFlow<RunInfo>(
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
        )
        val pendingRunIdFlow = MutableStateFlow(0)

        coroutineScope {
            launchICPCServices(rawRunsFlow, contestInfoFlow)
            launch(Dispatchers.IO) { reloadContestInfo(rawContestInfoFlow, contestInfoFlow, 30.seconds) }
            launch(Dispatchers.IO) { fetchNewRunsOnly(rawContestInfoFlow, runsBufferFlow, pendingRunIdFlow, 1.seconds) }
            launch(Dispatchers.IO) { reloadAllRuns(rawContestInfoFlow, runsBufferFlow, 120.seconds) }
            launch { RunsBufferService(runsBufferFlow, rawRunsFlow).run() }
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
        return ContestParseResult(contestInfo, submissions)
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
        pendingRunIdFlow: MutableStateFlow<Int>,
        period: Duration
    ) {
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
                    if (pageSubmissions.isEmpty() || pageSubmissions.last().id <= pendingRunIdFlow.value) {
                        break
                    }
                    page++
                }
                pendingRunIdFlow.value = runs.filter { !it.isJudged }.minOfOrNull { it.id }
                    ?: runs.maxOfOrNull { it.id } ?: 0
                runsBufferFlow.emit(runs)
            } catch (e: IOException) {
                log.error("Failed to reload rejudges", e)
            }
            delay(period)
        }
    }
}
