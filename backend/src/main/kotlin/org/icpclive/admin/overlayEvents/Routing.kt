package org.icpclive.admin.overlayEvents

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.DataBus
import org.icpclive.admin.Urls
import org.icpclive.background.BackgroundProcessManager
import java.util.*
import kotlin.collections.ArrayDeque


fun Routing.configureOverlayEvents() : Urls {
    val urls = object : Urls {
        override val mainPage = "/admin/overlayEvents"
    }
    val events = ArrayDeque<String>()
    val mutex = Mutex()
    // TODO: looks like replay cache, but how to get in on merge flow?
    BackgroundProcessManager.launch {
        DataBus.allEvents.collect {
            mutex.withLock {
                events.add(Json.encodeToString(it))
                if (events.size >= 30) {
                    events.removeFirst()
                }
            }
        }
    }
    get(urls.mainPage) {
        val eventsCopy = mutex.withLock { events.toList() }
        call.respondHtml {
            overlayEventsView(eventsCopy)
        }
    }
    return urls
}