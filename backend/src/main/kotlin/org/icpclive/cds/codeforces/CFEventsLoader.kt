package org.icpclive.cds.codeforces

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.icpclive.api.RunInfo
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContestPhase
import org.icpclive.cds.codeforces.api.data.CFSubmission
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.config.Config.loadProperties
import org.icpclive.data.DataBus
import org.icpclive.service.EmulationService
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.RunsBufferService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.utils.humanReadable
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * @author egor@egork.net
 */
class CFEventsLoader {
    private val contestInfo = CFContestInfo()
    private val central: CFApiCentral

    init {
        val properties = loadProperties("events")
        central = CFApiCentral(properties.getProperty("contest_id").toInt())
        if (properties.containsKey(CF_API_KEY_PROPERTY_NAME) && properties.containsKey(CF_API_SECRET_PROPERTY_NAME)) {
            central.setApiKeyAndSecret(
                properties.getProperty(CF_API_KEY_PROPERTY_NAME),
                properties.getProperty(CF_API_SECRET_PROPERTY_NAME)
            )
        }
    }

    suspend fun run() {
        val contestInfoFlow = MutableStateFlow(contestInfo.toApi()).also { DataBus.contestInfoUpdates.complete(it) }
        val standingsLoader = object : RegularLoaderService<CFStandings>() {
            override val url
                get() = central.standingsUrl
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
            override val url
                get() = central.statusUrl
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
        val properties: Properties = loadProperties("events")
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

        if (emulationSpeedProp != null) {
            contestInfo.updateStandings(standingsLoader.loadOnce())
            val runs = contestInfo.parseSubmissions(statusLoader.loadOnce().list)
            coroutineScope {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                log.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${emulationStartTime.humanReadable}")
                val rawRunsFlow = MutableSharedFlow<RunInfo>(
                    extraBufferCapacity = 100000,
                    onBufferOverflow = BufferOverflow.SUSPEND
                )
                launch {
                    EmulationService(
                        emulationStartTime,
                        emulationSpeed,
                        runs,
                        contestInfo.toApi(),
                        contestInfoFlow,
                        rawRunsFlow
                    ).run()
                }
                launchICPCServices(contestInfo.problemsNumber, rawRunsFlow)
            }
        } else {
            coroutineScope {
                val standingsFlow = MutableStateFlow<CFStandings?>(null)
                val statusFlow = MutableStateFlow(CFSubmissionList(emptyList()))
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
                val standingsRunning = standingsFlow
                    .filterNotNull()
                    .dropWhile { it.contest.phase == CFContestPhase.BEFORE }
                    .first()
                launchICPCServices(standingsRunning.problems.size, rawRunsFlow)
                launch(Dispatchers.IO) { statusLoader.run(statusFlow, 5.seconds) }


                merge(standingsFlow.filterNotNull(), statusFlow).collect {
                    when (it) {
                        is CFStandings -> {
                            contestInfo.updateStandings(it)
                            contestInfoFlow.value = contestInfo.toApi()
                        }
                        is CFSubmissionList -> {
                            val submissions = contestInfo.parseSubmissions(it.list)
                            log.info("Loaded ${submissions.size} runs")
                            runsBufferFlow.emit(submissions)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private val log = getLogger(CFEventsLoader::class)
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}