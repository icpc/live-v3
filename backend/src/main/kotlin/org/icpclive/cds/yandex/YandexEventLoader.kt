package org.icpclive.cds.yandex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
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
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchEmulation
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.OAuthAuth
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.cds.yandex.YandexConstants.API_BASE
import org.icpclive.cds.yandex.YandexConstants.TOKEN_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.CONTEST_ID_PROPERTY_NAME
import org.icpclive.cds.yandex.YandexConstants.LOGIN_PREFIX_PROPERTY_NAME
import org.icpclive.cds.yandex.api.ContestDescription
import org.icpclive.cds.yandex.api.Participant
import org.icpclive.cds.yandex.api.Problem
import org.icpclive.cds.yandex.api.Problems
import org.icpclive.cds.yandex.api.Submission
import org.icpclive.cds.yandex.api.Submissions
import org.icpclive.service.RunsBufferService
import java.io.IOException
import java.util.Properties
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexEventLoader  {
    val apiKey: String
    val loginPrefix: String
    val contestId: Long
    val httpClient: HttpClient
    val timeExtractingService: TimeExtractingService

    private val formatter = Json {
        ignoreUnknownKeys  = true
    }

    private val contestDescriptionLoader: RegularLoaderService<ContestDescription>
    private val problemLoader: RegularLoaderService<List<Problem>>
    private val participantLoader: RegularLoaderService<List<Participant>>
    private val allSubmissionsLoader: RegularLoaderService<List<Submission>>


    init {
        val props = Config.loadProperties("events")
        apiKey = props.getProperty(TOKEN_PROPERTY_NAME)
        contestId = props.getProperty(CONTEST_ID_PROPERTY_NAME).toLong()
        loginPrefix = props.getProperty(LOGIN_PREFIX_PROPERTY_NAME)

        // TODO: Java engine (requires Java 11 or higher)
        // TODO: use ktor everywhere instead of URL.openConnection
        httpClient = HttpClient(Apache) {
            defaultRequest {
                url("$API_BASE/contests/$contestId/")
                header("Authorization", "OAuth $apiKey")
            }

            engine {
                threadsCount = 2
            }
        }

        timeExtractingService = TimeExtractingService(httpClient)

        contestDescriptionLoader = object : RegularLoaderService<ContestDescription>() {
            override val url = "$API_BASE/contests/$contestId"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                    formatter.decodeFromString<ContestDescription>(data)
        }

        problemLoader = object : RegularLoaderService<List<Problem>>() {
            override val url = "$API_BASE/contests/$contestId/problems"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                    formatter.decodeFromString<Problems>(data).problems.sortedBy { it.alias }
        }

        participantLoader = object : RegularLoaderService<List<Participant>>() {
            override val url = "$API_BASE/contests/$contestId/participants"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                    formatter.decodeFromString<List<Participant>>(data).filter { it.login.startsWith(loginPrefix) }
        }

        allSubmissionsLoader = object : RegularLoaderService<List<Submission>>() {
            override val url = "$API_BASE/contests/$contestId/submissions?locale=ru&page=1&pageSize=100000"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                    formatter.decodeFromString<Submissions>(data).submissions.reversed()
        }
    }

    suspend fun run() {
        val properties: Properties = Config.loadProperties("events")
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

        val rawContestInfo = YandexContestInfo(
            contestDescriptionLoader.loadOnce(),
            problemLoader.loadOnce(),
            participantLoader.loadOnce()
        )
        val contestInfo = rawContestInfo.toApi()

        if (emulationSpeedProp != null) {
            coroutineScope {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                log.info("It will take a long time, please wait patiently...")
                launchEmulation(
                    emulationStartTime, emulationSpeed,
                    allSubmissionsLoader.loadOnce().filter(rawContestInfo::isTeamSubmission).map {
                        rawContestInfo.submissionToRun(it, timeExtractingService.getTime(it.id))
                    },
                    contestInfo
                )
                log.info("Loaded all submissions for emulation")
            }
        } else {
            val rawContestInfoFlow = MutableStateFlow(rawContestInfo)
            val contestInfoFlow = MutableStateFlow(contestInfo).also { DataBus.contestInfoUpdates.complete(it) }

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

    }
    companion object {
        private val log = getLogger(YandexEventLoader::class)
    }

    // TODO: try .stateIn
    suspend fun reloadContestInfo(
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

    suspend fun reloadAllRuns(
            rawContestInfoFlow: MutableStateFlow<YandexContestInfo>,
            runsBufferFlow: MutableSharedFlow<List<RunInfo>>,
            period: Duration
    ) {
        while (true) {
            try {
                val rawContestInfo = rawContestInfoFlow.value
                val submissions = allSubmissionsLoader.loadOnce().filter(rawContestInfo::isTeamSubmission).map {
                    rawContestInfo.submissionToRun(it, timeExtractingService.getTime(it.id))
                }
                runsBufferFlow.emit(submissions)
            } catch (e: IOException) {
                log.error("Failed to reload rejudges", e)
            }
            delay(period)
        }
    }

    suspend fun fetchNewRunsOnly(
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
                    runs.addAll(pageSubmissions.filter(rawContestInfo::isTeamSubmission).map {
                        rawContestInfo.submissionToRun(it, timeExtractingService.getTime(it.id))
                    })
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