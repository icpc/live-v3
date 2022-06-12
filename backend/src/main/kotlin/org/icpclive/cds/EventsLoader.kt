package org.icpclive.cds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.icpclive.cds.codeforces.CFEventsLoader
import org.icpclive.cds.pcms.PCMSEventsLoader
import org.icpclive.cds.clics.ClicsEventsLoader
import org.icpclive.cds.yandex.YandexEventLoader
import org.icpclive.config.Config.loadProperties

fun CoroutineScope.launchEventsLoader() {
    val properties = loadProperties("events")
    launch {
        when (val standingsType = properties.getProperty("standings.type")) {
            "CLICS" -> ClicsEventsLoader().run()
            "PCMS" -> PCMSEventsLoader().run()
            "CF" -> CFEventsLoader().run()
            "YANDEX" -> YandexEventLoader().run()
            else -> throw IllegalArgumentException("Unknown standings.type $standingsType")
        }
    }
}
