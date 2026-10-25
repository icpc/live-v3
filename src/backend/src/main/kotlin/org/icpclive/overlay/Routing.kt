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
import kotlin.time.Duration.Companion.milliseconds

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

context(dataBus: DataBus)
private inline fun <reified T : Any> Route.setUpScoreboard(crossinline getter: suspend DataBus.(OptimismLevel) -> Flow<T>) {
    flowEndpoint("/normal") { dataBus.getter(OptimismLevel.NORMAL) }
    flowEndpoint("/optimistic") { dataBus.getter(OptimismLevel.OPTIMISTIC) }
    flowEndpoint("/pessimistic") { dataBus.getter(OptimismLevel.PESSIMISTIC) }
}

context(dataBus: DataBus)
fun Route.configureOverlayRouting() {
    flowEndpoint("/mainScreen") { dataBus.mainScreenFlow.await() }
    flowEndpoint("/contestInfo") { dataBus.currentContestInfoFlow() }
    flowEndpoint("/runs") { dataBus.contestStateFlow.await().map { it.runsAfterEvent.values.sortedBy { it.time } } }
    flowEndpoint("/teamRuns/{id}") { call ->
        val teamIdStr = call.parameters["id"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Invalid team id")
            null
        } else {
            val teamId = teamIdStr.toTeamId()
            dataBus.timelineFlow.await()
                .map { it[teamId] }
                .distinctUntilChanged { a, b -> a === b }
                .map { it ?: emptyList() }
        }
    }
    flowEndpoint("/queue") { dataBus.queueFlow.await() }
    flowEndpoint("/statistics") { dataBus.statisticFlow.await() }
    flowEndpoint("/ticker") { dataBus.tickerFlow.await() }
    route("/scoreboard") {
        setUpScoreboard { getScoreboardDiffs(it) }
    }
    route("/svgAchievement") {
        configureSvgAchievementRouting(Config.mediaDirectory)
    }
    get("/visualConfig.json") { call.respond(dataBus.visualConfigFlow.await().value) }

    get("/teamKeylog/{id}") {
        val teamIdStr = call.parameters["id"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Invalid team id")
            return@get
        }
        val intervalMs = call.request.queryParameters["intervalMs"]?.toLongOrNull()
        if (intervalMs == null || intervalMs <= 0) {
            call.respond(HttpStatusCode.BadRequest, "Missing or invalid intervalMs query parameter")
            return@get
        }
        val result = dataBus.keylogService.await().getKeylog(teamIdStr.toTeamId(), intervalMs.milliseconds)
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(result)
        }
    }
}
