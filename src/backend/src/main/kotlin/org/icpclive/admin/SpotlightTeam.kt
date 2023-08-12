package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.combine
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
        sendJsonFlow(DataBus.teamInterestingFlow.await().combine(DataBus.contestInfoFlow.await()) { teams, info ->
            val teamsScore = teams.associate { it.teamId to it.score }
            info.teams.map { InterestingTeam(it.id, it.fullName, teamsScore[it.id] ?: 0.0) }
        })
    }
}
