package org.icpclive.overlay

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.admin.getExternalRun
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfoFlow
import org.icpclive.util.sendJsonFlow
import kotlin.time.Duration

inline fun <reified T : Any> Route.flowEndpoint(name: String, crossinline dataProvider: suspend () -> Flow<T>) {
    webSocket(name) { sendJsonFlow(dataProvider()) }
    get(name) { call.respond(dataProvider().first()) }
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
    webSocket("/teamRuns/{id}") {
        val teamIdStr = call.parameters["id"]
        if (teamIdStr.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid team id"))
            return@webSocket
        }
        val teamId = teamIdStr.toTeamId()
        val acceptedProblems = mutableSetOf<ProblemId>()
        val allRuns = mutableMapOf<RunId, RunInfo>()
        DataBus.contestStateFlow.await().first().runsAfterEvent.values
            .filter { teamId == it.teamId && it.time != Duration.ZERO }
            .forEach { allRuns[it.id] = it }
        sendJsonFlow(DataBus.contestStateFlow.await()
            .mapNotNull { (it.lastEvent as? RunUpdate)?.newInfo }
            .runningFold(allRuns.toPersistentMap()) { acc, it ->
                if (it.teamId == teamId) acc.put(it.id, it) else if (it.id in acc) acc.remove(it.id) else acc
            }
            .distinctUntilChanged { a, b -> a === b }
            .map { runs -> runs.values.sortedBy { it.time } }
            .map { runs ->
                acceptedProblems.clear()
                runs.mapNotNull { info -> TimeLineRunInfo.fromRunInfo(info, acceptedProblems) }
            })
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
    get("/externalRun/{id}") {
        val runInfo = getExternalRun(call.parameters["id"]!!.toRunId())
        if (runInfo == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(runInfo)
        }
    }
}
