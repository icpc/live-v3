package org.icpclive.overlay

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.api.toTeamId
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfoFlow
import org.icpclive.util.sendJsonFlow

inline fun <reified T : Any> Route.flowEndpoint(name: String, crossinline dataProvider: suspend (ApplicationCall) -> Flow<T>?) {
    webSocket(name) {
        val flow = dataProvider(call) ?: return@webSocket
        sendJsonFlow(flow)
    }
    get(name) {
        val result = dataProvider(call)?.first() ?: return@get
        call.respond(result)
    }
}

private inline fun <reified T : Any> Route.setUpScoreboard(crossinline getter: suspend DataBus.(OptimismLevel) -> Flow<T>) {
    flowEndpoint("/normal") { DataBus.getter(OptimismLevel.NORMAL) }
    flowEndpoint("/optimistic") { DataBus.getter(OptimismLevel.OPTIMISTIC) }
    flowEndpoint("/pessimistic") { DataBus.getter(OptimismLevel.PESSIMISTIC) }
}

fun Route.configureOverlayRouting() {
    flowEndpoint("/mainScreen") { DataBus.mainScreenFlow.await() }
    flowEndpoint("/contestInfo") { DataBus.currentContestInfoFlow() }
    flowEndpoint("/runs") { DataBus.contestStateFlow.await().map { it.runsAfterEvent.values.sortedBy { it.time } } }
    flowEndpoint("/teamRuns/{id}") { call ->
        val teamIdStr = call.parameters["id"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Invalid team id")
            null
        } else {
            val teamId = teamIdStr.toTeamId()
            DataBus.timelineFlow.await()
                .map { it[teamId] }
                .distinctUntilChanged { a, b -> a === b }
                .map { it ?: emptyList() }
        }
    }
    flowEndpoint("/queue") { DataBus.queueFlow.await() }
    flowEndpoint("/statistics") { DataBus.statisticFlow.await() }
    flowEndpoint("/ticker") { DataBus.tickerFlow.await() }
    route("/scoreboard") {
        setUpScoreboard { getScoreboardDiffs(it) }
    }
    route("/svgAchievement") {
        configureSvgAchievementRouting(Config.mediaDirectory)
    }
    get("/visualConfig.json") { call.respond(DataBus.visualConfigFlow.await().value) }
}
