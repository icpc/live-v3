package org.icpclive.cds

import org.icpclive.Config.loadProperties
import org.icpclive.cds.pcms.PCMSEventsLoader
import org.icpclive.cds.wf.json.WFEventsLoader
import org.icpclive.cds.codeforces.CFEventsLoader

abstract class EventsLoader : Runnable {
    var emulationSpeed = 0.0
        protected set
    var emulationEnabled = false
        protected set
    protected var emulationStartTime: Long = 0

    companion object {
        val instance by lazy {
            val properties = loadProperties("events")
            val standingsType = properties.getProperty("standings.type")
            when (standingsType) {
                "WF" -> WFEventsLoader(false)
                "WFRegionals" -> WFEventsLoader(true)
                "PCMS" -> PCMSEventsLoader()
                "CF" -> CFEventsLoader()
                else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
            }
        }
    }
}