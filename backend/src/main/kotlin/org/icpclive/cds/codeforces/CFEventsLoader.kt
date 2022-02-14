package org.icpclive.cds.codeforces

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus
import org.icpclive.api.RunInfo
import org.icpclive.api.Scoreboard
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.OptimismLevel
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.service.RunsBufferService
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
        central = CFApiCentral(
            properties.getProperty("contest_id").toInt()
        )
        if (properties.containsKey(CF_API_KEY_PROPERTY_NAME) && properties.containsKey(CF_API_SECRET_PROPERTY_NAME)) {
            central.setApiKeyAndSecret(
                properties.getProperty(CF_API_KEY_PROPERTY_NAME), properties.getProperty(
                    CF_API_SECRET_PROPERTY_NAME
                )
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
                launch { RunsBufferService(runsBufferFlow).run() }
                while (true) {
                    val standings = central.standings ?: continue
                    val submissions =
                        if (standings.contest.phase == CFContest.CFContestPhase.BEFORE) null else central.status
                    log.info("Data received")
                    contestInfo.update(standings, submissions)
                    DataBus.contestInfoFlow.emit(contestInfo.toApi())
                    runsBufferFlow.emit(contestInfo.runs.map { it.toApi() })
                    DataBus.scoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.NORMAL))
                    DataBus.optimisticScoreboardFlow.value =
                        Scoreboard(contestInfo.getStandings(OptimismLevel.OPTIMISTIC))
                    DataBus.pessimisticScoreboardFlow.value =
                        Scoreboard(contestInfo.getStandings(OptimismLevel.PESSIMISTIC))
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