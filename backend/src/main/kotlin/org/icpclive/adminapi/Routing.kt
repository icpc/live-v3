package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.*

suspend inline fun ApplicationCall.adminApiAction(block: ApplicationCall.() -> Unit) = try {
    block()
    respond(mapOf("status" to "ok"))
} catch (e: AdminActionApiException) {
    respond(mapOf("status" to "error", "message" to e.message))
}


fun Application.configureAdminApiRouting() {
    fun path(name: String) = environment.config.property("live.presets.$name").getString()
    routing {
        route("/adminapi") {
            route("/advertisement") { setupPresetWidgetRouting(path("advertisements"), ::AdvertisementWidget) }
            route("/picture") { setupPresetWidgetRouting(path("pictures"), ::PictureWidget) }
            route("/scoreboard") { setupSimpleWidgetRouting(ScoreboardSettings(), ::ScoreboardWidget) }
            route("/queue") { setupSimpleWidgetRouting(QueueSettings(), ::QueueWidget) }
            route("/statistics") { setupSimpleWidgetRouting(StatisticsSettings(), ::StatisticsWidget) }
            route("/ticker") { setupSimpleWidgetRouting(TickerSettings(), ::TickerWidget) }
        }
    }
}