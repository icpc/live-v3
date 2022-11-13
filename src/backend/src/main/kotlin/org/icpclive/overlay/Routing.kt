package org.icpclive.overlay

import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.icpclive.api.OptimismLevel
import org.icpclive.data.DataBus
import org.icpclive.util.sendJsonFlow

fun Route.configureOverlayRouting() {
    webSocket("/mainScreen") { sendJsonFlow(DataBus.mainScreenFlow.await()) }
    webSocket("/contestInfo") { sendJsonFlow(DataBus.contestInfoFlow.await()) }
    webSocket("/queue") { sendJsonFlow(DataBus.queueFlow.await()) }
    webSocket("/statistics") { sendJsonFlow(DataBus.statisticFlow.await()) }
    webSocket("/ticker") { sendJsonFlow(DataBus.tickerFlow.await()) }
    route("/scoreboard") {
        webSocket("/normal") { sendJsonFlow(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
        webSocket("/optimistic") { sendJsonFlow(DataBus.getScoreboardEvents(OptimismLevel.OPTIMISTIC)) }
        webSocket("/pessimistic") { sendJsonFlow(DataBus.getScoreboardEvents(OptimismLevel.PESSIMISTIC)) }
    }
}
