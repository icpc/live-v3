package org.icpclive.cds.codeforces

import org.icpclive.Config.loadProperties
import org.icpclive.DataBus.publishContestInfo
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContest
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    override fun run() {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        scheduler.scheduleAtFixedRate({
            val standings = central.standings ?: return@scheduleAtFixedRate
            val submissions = if (standings.contest.phase == CFContest.CFContestPhase.BEFORE) null else central.status
            log.info("Data received")
            contestInfo.update(standings, submissions)
            publishContestInfo(contestInfo)
        }, 0, 5, TimeUnit.SECONDS)
        try {
            if (!scheduler.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                log.error("Scheduler in CFEventsLoader finished by timeout")
            }
        } catch (e: InterruptedException) {
            // ignored
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