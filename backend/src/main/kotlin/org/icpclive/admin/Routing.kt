package org.icpclive.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.icpclive.api.*
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.utils.sendFlow
import org.icpclive.utils.sendJsonFlow
import java.nio.file.Paths

suspend inline fun ApplicationCall.adminApiAction(block: ApplicationCall.() -> Unit) = try {
    val user = principal<User>()!!
    if (!user.confirmed) throw AdminActionApiException("Your account is not confirmed yet")
    application.log.info("Changing request ${request.path()} is done by ${user.name}")
    block()
    respond(mapOf("status" to "ok"))
} catch (e: AdminActionApiException) {
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}


fun Route.configureAdminApiRouting() {
    val presetsDirectory =
        Config.configDirectory.resolve(application.environment.config.property("live.presetsDirectory").getString())
    val mediaDirectory =
        Config.configDirectory.resolve(application.environment.config.property("live.mediaDirectory").getString())
    presetsDirectory.toFile().mkdirs()
    mediaDirectory.toFile().mkdirs()
    fun path(name: String) = Paths.get(presetsDirectory.toString(), "$name.json")
    authenticate("admin-api-auth") {
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
        route("/title") {
            setupPresetWidgetRouting(path("title")) { titleSettings: TitleSettings ->
                SvgWidget(
                    SvgTransformer(mediaDirectory, titleSettings.preset, titleSettings.data).toBase64()
                )
            }
        }
        route("/picture") { setupPresetWidgetRouting(path("pictures"), ::PictureWidget) }
        route("/tickerMessage") { setupPresetTickerRouting(path("ticker"), TickerMessageSettings::toMessage) }
        route("/users") { setupUserRouting() }
        get("/advancedProperties") { run { call.respond(DataBus.advancedPropertiesFlow.await().value) } }
        webSocket("/advancedProperties") { sendJsonFlow(DataBus.advancedPropertiesFlow.await()) }
        webSocket("/backendLog") { sendFlow(DataBus.loggerFlow) }
    }
}