package org.icpclive.admin.overlayEvents

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.DataBus
import org.icpclive.admin.Urls


fun Routing.configureOverlayEvents() : Urls {
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