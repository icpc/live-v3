package org.icpclive.overlay

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.api.OptimismLevel
import org.icpclive.data.DataBus
import org.icpclive.scoreboard.ScoreboardAndContestInfo
import org.icpclive.scoreboard.toLegacyScoreboard
import org.icpclive.util.sendJsonFlow

private inline fun <reified T> Route.setUpScoreboard(crossinline process: (Flow<ScoreboardAndContestInfo>) -> Flow<T>) {
    webSocket("/normal") { sendJsonFlow(process(DataBus.getScoreboardEvents(OptimismLevel.NORMAL))) }
    webSocket("/optimistic") { sendJsonFlow(process(DataBus.getScoreboardEvents(OptimismLevel.OPTIMISTIC))) }
    webSocket("/pessimistic") { sendJsonFlow(process(DataBus.getScoreboardEvents(OptimismLevel.PESSIMISTIC))) }
}

fun Route.configureOverlayRouting() {
    webSocket("/mainScreen") { sendJsonFlow(DataBus.mainScreenFlow.await()) }
    webSocket("/contestInfo") { sendJsonFlow(DataBus.contestInfoFlow.await()) }
    webSocket("/queue") { sendJsonFlow(DataBus.queueFlow.await()) }
    webSocket("/statistics") { sendJsonFlow(DataBus.statisticFlow.await()) }
    webSocket("/ticker") { sendJsonFlow(DataBus.tickerFlow.await()) }
    route("/scoreboard") {
        setUpScoreboard { flow -> flow.map { it.scoreboardSnapshot.toLegacyScoreboard(it.info) } }
        route("v2") {
            setUpScoreboard { flow ->
                flow.withIndex().map {
                    if (it.index == 0) it.value.scoreboardSnapshot else it.value.scoreboardDiff
                }
            }
        }
    }
    route("/svgAchievement"){
        configureSvgAtchievementRouting(Config.mediaDirectory)
    }
    get("/visualConfig.json") { call.respond(DataBus.visualConfigFlow.await().value) }
}
