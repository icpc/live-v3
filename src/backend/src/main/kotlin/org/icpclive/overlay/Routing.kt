package org.icpclive.overlay

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.admin.getExternalRun
import org.icpclive.cds.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfoFlow
import org.icpclive.util.sendJsonFlow

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
    webSocket("/teamRuns") {
        val teamIdStr = (incoming.receive() as? Frame.Text)?.readText()
        if (teamIdStr.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid team id"))
            return@webSocket
        }
        val teamId = teamIdStr.toTeamId()
        sendJsonFlow(DataBus.contestStateFlow.await().map { state ->
            state.runsAfterEvent.values.filter { it.teamId == teamId }
                .sortedBy { it.time }
        }.distinctUntilChanged()
            .map { runs ->
                val acceptedProblems = mutableSetOf<ProblemId>()
                runs.map { info ->
                    when (info.result) {
                        is RunResult.ICPC -> {
                            val icpcResult = info.result as RunResult.ICPC
                            if (!acceptedProblems.contains(info.problemId)) {
                                if (icpcResult.verdict == Verdict.Accepted) {
                                    acceptedProblems.add(info.problemId)
                                }
                                TimeLineRunInfo.ICPC(info.time, info.problemId, icpcResult.verdict.isAccepted)
                            } else {
                                null
                            }
                        }

                        is RunResult.IOI -> {
                            val ioiResult = info.result as RunResult.IOI
                            TimeLineRunInfo.IOI(info.time, info.problemId, ioiResult.scoreAfter)
                        }

                        else -> {
                            TimeLineRunInfo.InProgress(info.time, info.problemId)
                        }
                    }
                }.filterNotNull()
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
