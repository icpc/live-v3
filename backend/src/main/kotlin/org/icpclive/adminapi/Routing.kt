package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.*
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

suspend inline fun ApplicationCall.adminApiAction(block: ApplicationCall.() -> Unit) = try {
    block()
    respond(mapOf("status" to "ok"))
} catch (e: AdminActionApiException) {
    respond(mapOf("status" to "error", "message" to e.message))
}


fun Application.configureAdminApiRouting() {
    val presetsDirectory = Path.of(Config.configDirectory, environment.config.property("live.presetsDirectory").getString())
    presetsDirectory.toFile().mkdirs()
    fun path(name: String) = Path.of(presetsDirectory.toString(), "$name.json")
    routing {
        route("/adminapi") {
            route("/scoreboard") { setupSimpleWidgetRouting(ScoreboardSettings(), ::ScoreboardWidget) }
            route("/queue") { setupSimpleWidgetRouting(QueueSettings(), ::QueueWidget) }
            route("/statistics") { setupSimpleWidgetRouting(StatisticsSettings(), ::StatisticsWidget) }
            route("/ticker") { setupSimpleWidgetRouting(TickerSettings(), ::TickerWidget) }
            route("/teamview") {
                setupSimpleWidgetRouting(TeamViewSettings(), ::TeamViewWidget) {
                    DataBus.contestInfoUpdates.value.teams
                }
            }

            route("/advertisement") { setupPresetWidgetRouting(path("advertisements"), ::AdvertisementWidget) }
            route("/picture") { setupPresetWidgetRouting(path("pictures"), ::PictureWidget) }
            route("/tickermessage") { setupPresetTickerRouting(path("ticker"), TickerMessageSettings::toMessage) }
        }
    }
}