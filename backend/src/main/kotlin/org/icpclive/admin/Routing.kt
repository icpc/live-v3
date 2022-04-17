package org.icpclive.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.*
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import java.nio.file.Paths

suspend inline fun ApplicationCall.adminApiAction(block: ApplicationCall.() -> Unit) = try {
    block()
    respond(mapOf("status" to "ok"))
} catch (e: AdminActionApiException) {
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}


fun Route.configureAdminApiRouting() {
    val presetsDirectory =
        Paths.get(Config.configDirectory, application.environment.config.property("live.presetsDirectory").getString())
    presetsDirectory.toFile().mkdirs()
    fun path(name: String) = Paths.get(presetsDirectory.toString(), "$name.json")
    route("/scoreboard") { setupSimpleWidgetRouting(ScoreboardSettings(), ::ScoreboardWidget) }
    route("/queue") { setupSimpleWidgetRouting(QueueSettings(), ::QueueWidget) }
    route("/statistics") { setupSimpleWidgetRouting(StatisticsSettings(), ::StatisticsWidget) }
    route("/ticker") { setupSimpleWidgetRouting(TickerSettings(), ::TickerWidget) }
    route("/teamView") {
        setupSimpleWidgetRouting(TeamViewSettings(), ::TeamViewWidget) {
            DataBus.contestInfoUpdates.await().value.teams
        }
    }

    route("/advertisement") { setupPresetWidgetRouting(path("advertisements"), ::AdvertisementWidget) }
    route("/picture") { setupPresetWidgetRouting(path("pictures"), ::PictureWidget) }
    route("/tickerMessage") { setupPresetTickerRouting(path("ticker"), TickerMessageSettings::toMessage) }
    route("/advancedProperties") {
        post("reload") {
            Config.reloadAdvancedProperties()
            call.respond(mapOf("status" to "ok"))
        }
        get {
            val result = Config.advancedProperties.keys().toList().filterIsInstance<String>()
                .map { mapOf("key" to it, "value" to (Config.advancedProperties.getProperty(it) ?: "")) }
            call.respond(result)
        }
    }
}