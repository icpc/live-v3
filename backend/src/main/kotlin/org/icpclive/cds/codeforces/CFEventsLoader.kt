package org.icpclive.cds.codeforces

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.cds.codeforces.api.data.CFContestPhase
import org.icpclive.cds.codeforces.api.data.CFContestType
import org.icpclive.cds.codeforces.api.data.CFSubmission
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.RunsBufferService
import org.icpclive.service.launchICPCServices
import org.slf4j.LoggerFactory
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

/**
 * @author egor@egork.net
 */
class CFEventsLoader : EventsLoader() {
    private val contestInfo = CFContestInfo()
    private val central: CFApiCentral

    init {
        val properties = loadProperties("events")
        emulationSpeed = 1.0
        central = CFApiCentral(properties.getProperty("contest_id").toInt())
        if (properties.containsKey(CF_API_KEY_PROPERTY_NAME) && properties.containsKey(CF_API_SECRET_PROPERTY_NAME)) {
            central.setApiKeyAndSecret(
                properties.getProperty(CF_API_KEY_PROPERTY_NAME),
                properties.getProperty(CF_API_SECRET_PROPERTY_NAME)
            )
        }
    }

    override suspend fun run() {
        val standingsLoader = object : RegularLoaderService<CFStandings>() {
            override val url = central.standingsUrl
            override val login = ""
            override val password = ""
            override fun processLoaded(data: String) = try {
                central.parseAndUnwrapStatus(data)
                    ?.let { Json.decodeFromJsonElement<CFStandings>(it) }
                    ?: throw IOException()
            } catch (e: SerializationException) {
                throw IOException(e)
            }
        }

        class CFSubmissionList(val list: List<CFSubmission>)

        val statusLoader = object : RegularLoaderService<CFSubmissionList>() {
            override val url = central.statusUrl
            override val login = ""
            override val password = ""
            override fun processLoaded(data: String) = try {
                central.parseAndUnwrapStatus(data)
                    ?.let { Json.decodeFromJsonElement<List<CFSubmission>>(it) }
                    ?.let { CFSubmissionList(it) }
                    ?: throw IOException()
            } catch (e: SerializationException) {
                throw IOException(e)
            }
        }
        val standingsFlow = MutableStateFlow<CFStandings?>(null)
        val statusFlow = MutableStateFlow(CFSubmissionList(emptyList()))
        coroutineScope {
            launch(Dispatchers.IO) { standingsLoader.run(standingsFlow, 5.seconds) }
            val runsBufferFlow = MutableSharedFlow<List<RunInfo>>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = 100000,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            launch { RunsBufferService(runsBufferFlow, rawRunsFlow).run() }
            val processedStandingsFlow = standingsFlow
                .filterNotNull()
                .onEach {
                    contestInfo.updateStandings(it)
                    DataBus.contestInfoFlow.value = contestInfo.toApi()
                }
            val standingsRunning = processedStandingsFlow
                .dropWhile { it.contest.phase == CFContestPhase.BEFORE }
                .first()
            launchICPCServices(standingsRunning.problems.size, rawRunsFlow)
            launch(Dispatchers.IO) { statusLoader.run(statusFlow, 5.seconds) }

            val processedStatusFlow = statusFlow.onEach {
                contestInfo.updateSubmissions(it.list)
                runsBufferFlow.emit(contestInfo.runs.map { it.toApi() })
            }

            merge(processedStandingsFlow, processedStatusFlow).collect {}
        }
    }

    val contestData: CFContestInfo
        get() = contestInfo

    companion object {
        private val log = LoggerFactory.getLogger(CFEventsLoader::class.java)
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
        val instance: CFEventsLoader
            get() {
                val eventsLoader = EventsLoader.instance
                check(eventsLoader is CFEventsLoader)
                return eventsLoader
            }
    }
}