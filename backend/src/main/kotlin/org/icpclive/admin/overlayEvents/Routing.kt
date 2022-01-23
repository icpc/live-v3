package org.icpclive.admin.overlayEvents

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import org.icpclive.EventManager
import org.icpclive.admin.Urls



fun Routing.configureOverlayEvents() : Urls {
    val urls = object : Urls {
        override val mainPage = "/admin/overlayEvents"
    }
    val listener = OverlayEventsListener()
    runBlocking {
        EventManager.registerListener(listener)
    }
    get(urls.mainPage) {
        val events = listener.getEventsToShow()
        call.respondHtml {
            overlayEventsView(events)
        }
    }
    return urls
}