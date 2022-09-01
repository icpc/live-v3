package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.first
import org.icpclive.config
import org.icpclive.data.DataBus
import org.icpclive.data.WidgetControllers
import org.icpclive.utils.sendFlow
import org.icpclive.utils.sendJsonFlow

fun Route.configureAdminApiRouting() {
    authenticate("admin-api-auth") {
        route("/queue") { setupController(WidgetControllers.queue) }
        route("/statistics") { setupController(WidgetControllers.statistics) }
        route("/ticker") { setupController(WidgetControllers.ticker) }
        route("/scoreboard") {
            setupController(WidgetControllers.scoreboard)
            get("/regions") {
                call.respond(getRegions())
            }
        }
        route("/teamView") {
            setupController(WidgetControllers.teamView)
            get("/teams") {
                call.respond(getTeams())
            }
        }
        route("/splitScreen") {
            setupController(WidgetControllers.splitScreen)
            get("/teams") {
                call.respond(getTeams())
            }
        }
        route("/teamPVP") {
            setupController(WidgetControllers.teamPVP)
            get("/teams") {
                call.respond(getTeams())
            }
        }
        route("/teamLocator") { setupController(WidgetControllers.locator) }


        route("/advertisement") { setupController(WidgetControllers.advertisement) }
        route("/picture") { setupController(WidgetControllers.picture) }
        route("/title") {
            setupController(WidgetControllers.title)
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
        route("/tickerMessage") { setupController(WidgetControllers.tickerMessage) }
        route("/analytics") { setupAnalytics() }

        route("/users") { setupUserRouting() }
        get("/advancedProperties") { run { call.respond(DataBus.advancedPropertiesFlow.await().first()) } }
        webSocket("/advancedProperties") { sendJsonFlow(DataBus.advancedPropertiesFlow.await()) }
        webSocket("/backendLog") { sendFlow(DataBus.loggerFlow) }
        webSocket("/adminActions") { sendFlow(DataBus.adminActionsFlow) }
    }
}
