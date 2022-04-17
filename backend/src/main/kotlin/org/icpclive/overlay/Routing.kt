package org.icpclive.overlay

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
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

fun Route.configureOverlayRouting() {
    webSocket("/mainScreen") { sendFlow(DataBus.mainScreenFlow.await()) }
    webSocket("/contestInfo") { sendFlow(DataBus.contestInfoUpdates.await()) }
    webSocket("/queue") { sendFlow(DataBus.queueFlow.await()) }
    webSocket("/statistics") { sendFlow(DataBus.statisticFlow.await()) }
    webSocket("/ticker") { sendFlow(DataBus.tickerFlow.await()) }
    route("/scoreboard") {
        webSocket("/normal") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
        webSocket("/optimistic") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.OPTIMISTIC)) }
        webSocket("/pessimistic") { sendFlow(DataBus.getScoreboardEvents(OptimismLevel.PESSIMISTIC)) }
    }
}
