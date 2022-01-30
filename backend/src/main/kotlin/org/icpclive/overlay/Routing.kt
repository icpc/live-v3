package org.icpclive.overlay

import io.ktor.application.*
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.DataBus

private suspend inline fun <reified T> DefaultWebSocketServerSession.sendFlow(flow: Flow<T>) {
    val sender = async {
        flow.collect {
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

fun Application.configureOverlayRouting() {
    routing {
        webSocket("/overlay/mainScreen") { sendFlow(DataBus.mainScreenEvents) }
        webSocket("/overlay/queue") { sendFlow(DataBus.queueEvents) }
        webSocket("/overlay/contestInfo") { sendFlow(DataBus.contestInfoFlow) }
        webSocket("/overlay/scoreboard/normal") { sendFlow(DataBus.scoreboardFlow) }
        webSocket("/overlay/scoreboard/optimistic") { sendFlow(DataBus.optimisticScoreboardFlow) }
        webSocket("/overlay/scoreboard/pessimistic") { sendFlow(DataBus.pessimisticScoreboardFlow) }
    }
}