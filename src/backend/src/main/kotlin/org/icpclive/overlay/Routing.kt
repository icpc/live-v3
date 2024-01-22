package org.icpclive.overlay

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.data.DataBus
import org.icpclive.cds.scoreboard.ScoreboardAndContestInfo
import org.icpclive.cds.scoreboard.toLegacyScoreboard
import org.icpclive.util.sendJsonFlow

inline fun <reified T: Any> Route.flowEndpoint(name: String, crossinline dataProvider: suspend () -> Flow<T>) {
    webSocket(name) { sendJsonFlow(dataProvider()) }
    get(name) { call.respond(dataProvider().first()) }
}

private inline fun <reified T : Any> Route.setUpScoreboard(crossinline process: (Flow<ScoreboardAndContestInfo>) -> Flow<T>) {
    flowEndpoint("/normal") { process(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
    flowEndpoint("/optimistic") { process(DataBus.getScoreboardEvents(OptimismLevel.OPTIMISTIC)) }
    flowEndpoint("/pessimistic") { process(DataBus.getScoreboardEvents(OptimismLevel.PESSIMISTIC)) }
}

fun Route.configureOverlayRouting() {
    flowEndpoint("/mainScreen") { DataBus.mainScreenFlow.await() }
    flowEndpoint("/contestInfo") { DataBus.contestInfoFlow.await() }
    flowEndpoint("/queue") { DataBus.queueFlow.await() }
    flowEndpoint("/statistics") { DataBus.statisticFlow.await() }
    flowEndpoint("/ticker") { DataBus.tickerFlow.await() }
    route("/scoreboard") {
        setUpScoreboard { flow -> flow.map { it.scoreboardSnapshot.toLegacyScoreboard(it.info) } }
        route("/v2") {
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
