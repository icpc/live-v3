package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import org.icpclive.api.AddTeamScoreRequest
import org.icpclive.api.InterestingTeam
import org.icpclive.data.DataBus
import org.icpclive.util.completeOrThrow
import org.icpclive.util.reliableSharedFlow
import org.icpclive.util.sendJsonFlow

fun Route.setupSpotlight() {
    val addScoreRequests = reliableSharedFlow<AddTeamScoreRequest>()
    DataBus.teamInterestingScoreRequestFlow.completeOrThrow(addScoreRequests)

    post("/addScore") {
        call.adminApiAction {
            val requests = call.safeReceive<List<AddTeamScoreRequest>>()
            requests.forEach { addScoreRequests.emit(it) }
        }
    }

    webSocket {
        sendJsonFlow(flow {
            combine(
                DataBus.teamInterestingFlow.await(),
                DataBus.contestInfoFlow.await(),
                ::Pair
            ).collect { (teams, info) ->
                val teamsScore = teams.associate { it.teamId to it.score }
                emit(info.teams.map { InterestingTeam(it.id, it.name, teamsScore[it.id] ?: 0.0) })
            }
        })
    }
}
