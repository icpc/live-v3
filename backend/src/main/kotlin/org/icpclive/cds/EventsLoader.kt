package org.icpclive.cds

import kotlinx.datetime.Instant
import org.icpclive.Config.loadProperties
import org.icpclive.cds.pcms.PCMSEventsLoader
import org.icpclive.cds.wf.json.WFEventsLoader
import org.icpclive.cds.codeforces.CFEventsLoader

abstract class EventsLoader {
    var emulationSpeed = 0.0
        protected set
    protected var emulationStartTime = Instant.fromEpochMilliseconds(0)

    abstract suspend fun run()

    companion object {
        val instance by lazy {
            val properties = loadProperties("events")
            when (val standingsType = properties.getProperty("standings.type")) {
                "WF" -> WFEventsLoader(false)
                "WFRegionals" -> WFEventsLoader(true)
                "PCMS" -> PCMSEventsLoader()
                "CF" -> CFEventsLoader()
                else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
            }
        }
    }
}