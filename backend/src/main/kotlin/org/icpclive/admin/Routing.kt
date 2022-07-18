package org.icpclive.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.icpclive.Config
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.sendFlow
import org.icpclive.utils.sendJsonFlow
import java.nio.file.Paths

@Serializable
class AdminActionResponse<T>(
    val status: String,
    val response: T? = null
)

suspend inline fun <reified T> ApplicationCall.adminApiAction(block: ApplicationCall.() -> T) = try {
    val user = principal<User>()!!
    if (!user.confirmed) throw AdminActionApiException("Your account is not confirmed yet")
    application.log.info("Changing request ${request.path()} is done by ${user.name}")
    val result = block()
    respondText(contentType = ContentType.Application.Json) {
        Json.encodeToString(
            AdminActionResponse.serializer(serializer()), when (result) {
                is Unit -> AdminActionResponse("ok")
                else -> AdminActionResponse("ok", result)
            }
        )
    }
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
        route("/queue") { setupSimpleWidgetRouting(QueueSettings(), ::QueueWidget) }
        route("/statistics") { setupSimpleWidgetRouting(StatisticsSettings(), ::StatisticsWidget) }
        route("/ticker") { setupSimpleWidgetRouting(TickerSettings(), ::TickerWidget) }
        route("/scoreboard") {
            setupSimpleWidgetRouting(ScoreboardSettings(), ::ScoreboardWidget)
            get("/regions") {
                call.respond(getRegions())
            }
        }

        route("/teamView") {
            setupSimpleWidgetRouting(TeamViewSettings()) { TeamViewWidget(it) }
            get("/teams") {
                call.respond(getTeams())
            }
        }

        route("/splitscreen") {
            for (position in TeamViewPosition.values()) {
                route("${position.ordinal}") {
                    setupSimpleWidgetRouting(TeamViewSettings()) { TeamViewWidget(it, position) }
                }
            }
            get("/teams") {
                call.respond(getTeams())
            }
        }

        route("/teamPVP") {
            setupSimpleWidgetRouting(TeamPVPSettings(), ::TeamPVPWidget)
            get ("/teams") {
                call.respond(getTeams())
            }
        }

        route("/advertisement") { setupPresetWidgetRouting(path("advertisements"), ::AdvertisementWidget) }
        route("/picture") { setupPresetWidgetRouting(path("pictures"), ::PictureWidget) }
        route("/title") {
            setupPresetTitleRouting(path("title")) { titleSettings: TitleSettings ->
                SvgWidget(
                    SvgTransformer(mediaDirectory, titleSettings.preset, titleSettings.data).toBase64()
                )
            }
            get("/templates") {
                run {
                    val mediaDirectoryFile = mediaDirectory.toFile()
                    call.respond(mediaDirectoryFile.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".svg") }
                        .map { it.relativeTo(mediaDirectoryFile).path }.toList()
                    )
                }
            }
        }
        route("/tickerMessage") { setupPresetTickerRouting(path("ticker"), TickerMessageSettings::toMessage) }
        route("/users") { setupUserRouting() }
        get("/advancedProperties") { run { call.respond(DataBus.advancedPropertiesFlow.await().first()) } }
        webSocket("/advancedProperties") { sendJsonFlow(DataBus.advancedPropertiesFlow.await()) }
        webSocket("/backendLog") { sendFlow(DataBus.loggerFlow) }
        webSocket("/adminActions") { sendFlow(DataBus.adminActionsFlow) }
        webSocket("/analyticsEvents") { sendJsonFlow(DataBus.analyticsEventFlow.await()) }
    }
}
