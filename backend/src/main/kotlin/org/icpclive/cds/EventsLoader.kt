package org.icpclive.cds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.icpclive.cds.codeforces.CFEventsLoader
import org.icpclive.cds.ejudge.EjudgeEventsLoader
import org.icpclive.cds.pcms.PCMSEventsLoader
import org.icpclive.cds.wf.json.WFEventsLoader
import org.icpclive.cds.yandex.YandexEventLoader
import org.icpclive.config.Config.loadProperties

fun CoroutineScope.launchEventsLoader() {
    val properties = loadProperties("events")
    launch {
        when (val standingsType = properties.getProperty("standings.type")) {
            "WF" -> WFEventsLoader(false).run()
            "WFRegionals" -> WFEventsLoader(true).run()
            "PCMS" -> PCMSEventsLoader().run()
            "CF" -> CFEventsLoader().run()
            "YANDEX" -> YandexEventLoader().run()
            "EJUDGE" -> EjudgeEventsLoader().run()
            else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
        }
    }
}