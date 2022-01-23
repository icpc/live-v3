package org.icpclive.overlay

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.*
import org.icpclive.api.*
import org.icpclive.listeners.*


fun Application.configureOverlayRouting() {
    routing {
        webSocket("/overlay/mainScreen") {
            val listener = object : EventListener {
                override suspend fun processEvent(e: Event) {
                    outgoing.send(Frame.Text(Json.encodeToString(e)))
                }
            }
            EventManager.registerListener(listener)
            try {
                for (ignored in incoming) {
                    ignored.let {}
                }
            } finally {
                EventManager.unregisterListener(listener)
            }
        }
    }
}