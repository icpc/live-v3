package org.icpclive.overlay

import io.ktor.server.application.*
import io.ktor.websocket.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.cds.OptimismLevel
import org.icpclive.data.DataBus
import org.icpclive.utils.defaultJsonSettings

private suspend inline fun <reified T> DefaultWebSocketServerSession.sendFlow(flow: Flow<T>) {
    val formatter = defaultJsonSettings()
    val sender = async {
        flow.collect {
            val text = Frame.Text(formatter.encodeToString(it))
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
        route("/overlay") {
            webSocket("/mainScreen") { sendFlow(DataBus.mainScreenFlow.get()) }
            webSocket("/contestInfo") { sendFlow(DataBus.contestInfoUpdates) }
            webSocket("/queue") { sendFlow(DataBus.queueFlow.get()) }
            webSocket("/statistics") { sendFlow(DataBus.statisticFlow.get()) }
            webSocket("/ticker") { sendFlow(DataBus.tickerFlow.get()) }
            route("/scoreboard") {
                webSocket("/normal") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
                webSocket("/optimistic") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.OPTIMISTIC)) }
                webSocket("/pessimistic") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.PESSIMISTIC)) }
            }
        }
    }
}
