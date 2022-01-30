package org.icpclive.events

import org.icpclive.Config.loadProperties
import java.lang.Runnable
import org.icpclive.events.EventsLoader
import kotlin.jvm.Synchronized
import java.util.Properties
import org.icpclive.events.WF.json.WFEventsLoader
import org.icpclive.events.PCMS.PCMSEventsLoader
import org.icpclive.events.codeforces.CFEventsLoader

abstract class EventsLoader : Runnable {
    var emulationSpeed = 0.0
        protected set
    @JvmField
    protected var emulationStartTime: Long = 0
    abstract val contestData: ContestInfo?

    companion object {
        @JvmStatic
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