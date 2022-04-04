package org.icpclive.admin.overlayEvents

import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.data.DataBus
import org.icpclive.admin.Urls


fun Routing.configureOverlayEvents(): Urls {
    val urls = object : Urls {
        override val mainPage = "/admin/overlayEvents"
    }
    val events = DataBus.allEvents.shareIn(application, SharingStarted.Eagerly, replay = 30)
    get(urls.mainPage) {
        val eventsCopy = events.replayCache.map { Json.encodeToString(it) }
        call.respondHtml {
            overlayEventsView(eventsCopy)
        }
    }
    return urls
}