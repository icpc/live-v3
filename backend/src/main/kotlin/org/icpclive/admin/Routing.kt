package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.first
import org.icpclive.api.TeamViewPosition
import org.icpclive.config
import org.icpclive.data.DataBus
import org.icpclive.data.Controllers
import org.icpclive.util.sendFlow
import org.icpclive.util.sendJsonFlow

fun Route.configureAdminApiRouting() {
    authenticate("admin-api-auth") {
        route("/queue") { setupController(Controllers.queue) }
        route("/statistics") { setupController(Controllers.statistics) }
        route("/ticker") { setupController(Controllers.ticker) }
        route("/scoreboard") {
            setupController(Controllers.scoreboard)
            get("/regions") {
                call.respond(getRegions())
            }
        }
        route("/teamView") {
            setupController(Controllers.teamView(TeamViewPosition.SINGLE_TOP_RIGHT))
            get("/teams") {
                call.respond(getTeams())
            }
        }
        fun Route.setupTeamViews(positions: List<TeamViewPosition>) {
            setupControllerGroup(positions.map { Controllers.teamView(it) })
            positions.forEach { position ->
                route("/${position.name}") { setupController(Controllers.teamView(position)) }
            }
            get("/teams") { call.respond(getTeams()) }
        }
        route("/splitScreen") {
            setupTeamViews(
                listOf(
                    TeamViewPosition.TOP_LEFT,
                    TeamViewPosition.TOP_RIGHT,
                    TeamViewPosition.BOTTOM_LEFT,
                    TeamViewPosition.BOTTOM_RIGHT
                )
            )
        }
        route("/teamPVP") {
            setupTeamViews(listOf(TeamViewPosition.PVP_TOP, TeamViewPosition.PVP_BOTTOM))
        }
        route("/teamLocator") { setupController(Controllers.locator) }


        route("/advertisement") { setupController(Controllers.advertisement) }
        route("/picture") { setupController(Controllers.picture) }
        route("/title") {
            setupController(Controllers.title)
            get("/templates") {
                run {
                    val mediaDirectoryFile = config.mediaDirectory.toFile()
                    call.respond(mediaDirectoryFile.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".svg") }
                        .map { it.relativeTo(mediaDirectoryFile).path }.toList()
                    )
                }
            }
        }
        route("/tickerMessage") { setupController(Controllers.tickerMessage) }
        route("/analytics") { setupAnalytics() }

        route("/users") { setupUserRouting(Controllers.userController) }
        get("/advancedProperties") { run { call.respond(DataBus.advancedPropertiesFlow.await().first()) } }
        webSocket("/advancedProperties") { sendJsonFlow(DataBus.advancedPropertiesFlow.await()) }
        webSocket("/backendLog") { sendFlow(DataBus.loggerFlow) }
        webSocket("/adminActions") { sendFlow(DataBus.adminActionsFlow) }
    }
}
