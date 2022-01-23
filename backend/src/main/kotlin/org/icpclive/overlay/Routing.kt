package org.icpclive.overlay

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.*
import org.icpclive.api.*
import org.icpclive.background.*
import org.slf4j.LoggerFactory


fun Application.configureOverlayRouting() {
    routing {
        webSocket("/overlay/mainScreen") {
            val sender = async {
                DataBus.allEvents.collect {
                    val text = Frame.Text(Json.encodeToString(it))
                    outgoing.send(text)
                }
            }
            try {
                for (ignored in incoming) {
                    ignored.let {}
                }
            } finally {
                sender.cancel()
            }
        }
    }
}