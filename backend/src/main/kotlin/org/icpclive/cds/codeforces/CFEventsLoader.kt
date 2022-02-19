package org.icpclive.cds.codeforces

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.cds.codeforces.api.data.CFContestType
import org.icpclive.service.RunsBufferService
import org.icpclive.service.launchICPCServices
import org.slf4j.LoggerFactory
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
        withContext(Dispatchers.IO) {
            coroutineScope {
                val runsBufferFlow = MutableSharedFlow<List<RunInfo>>(
                    extraBufferCapacity = 16,
                    onBufferOverflow = BufferOverflow.DROP_OLDEST
                )
                val rawRunsFlow = MutableSharedFlow<RunInfo>(
                    extraBufferCapacity = 100000,
                    onBufferOverflow = BufferOverflow.SUSPEND
                )
                launch { RunsBufferService(runsBufferFlow, rawRunsFlow).run() }
                var servicesLaunched = false
                while (true) {
                    val standings = central.standings ?: continue
                    contestInfo.updateStandings(standings)
                    if (contestInfo.status != ContestStatus.RUNNING || contestInfo.status == ContestStatus.OVER) {
                        val submissions = central.status
                        if (submissions != null) {
                            contestInfo.updateSubmissions(submissions)
                        }
                    }
                    if (!servicesLaunched && contestData.problems.isNotEmpty()) {
                        if (standings.contest.type != CFContestType.ICPC) {
                            throw IllegalStateException("Only ICPC contests are supported for now")
                        }
                        launchICPCServices(
                            contestData.problems.size,
                            rawRunsFlow
                        )
                        servicesLaunched = true
                    }
                    DataBus.contestInfoFlow.value = contestInfo.toApi()
                    runsBufferFlow.emit(contestInfo.runs.map { it.toApi() })
                    delay(5.seconds)
                }
            }
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