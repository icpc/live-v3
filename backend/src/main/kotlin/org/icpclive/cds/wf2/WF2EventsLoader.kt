package org.icpclive.cds.wf2

import kotlinx.coroutines.coroutineScope
import org.icpclive.config.Config
import org.icpclive.service.RegularLoaderService
import org.icpclive.utils.NetworkUtils.prepareNetwork
import org.icpclive.utils.processCreds

class WF2EventsLoader {
    private val properties = Config.loadProperties("events")
    private val central = WF2ApiCentral(properties)

    suspend fun run() {
        coroutineScope {
            println("Aboba")
        }
    }
}
