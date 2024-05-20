package org.icpclive.admin

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.Config
import org.icpclive.api.TeamViewPosition
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.cds.tunning.toAdvancedProperties
import org.icpclive.data.*
import org.icpclive.util.sendFlow
import java.nio.file.Files
import kotlin.io.path.notExists

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
            setupControllerGroup(positions.associate { it.name to Controllers.teamView(it) })
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
        route("/fullScreenClock") { setupController(Controllers.fullScreenClock) }
        route("/teamLocator") { setupController(Controllers.locator) }


        route("/advertisement") { setupController(Controllers.advertisement) }
        route("/picture") { setupController(Controllers.picture) }
        route("/title") {
            setupController(Controllers.title)
            get("/templates") {
                run {
                    val mediaDirectoryFile = Config.mediaDirectory.toFile()
                    call.respond(mediaDirectoryFile.walkTopDown()
                        .filter { it.isFile && it.name.endsWith(".svg") }
                        .map { it.relativeTo(mediaDirectoryFile).path }.toList()
                    )
                }
            }
        }
        route("/tickerMessage") { setupController(Controllers.tickerMessage) }
        route("/analytics") { setupAnalytics() }

        route("/teamSpotlight") { setupSpotlight() }

        route("/users") { setupUserRouting(Controllers.userController) }
        get("/advancedJsonPreview") {
            val formatter = Json {
                prettyPrint = true
                encodeDefaults = false
            }
            run {
                call.respondText(contentType = ContentType.Application.Json) {
                    formatter.encodeToString(DataBus.currentContestInfo().toAdvancedProperties(
                        call.request.queryParameters["fields"]?.split(",")?.toSet() ?: emptySet()
                    ))
                }
            }
        }

        route("/advancedJson") {
            get {
                if (Config.cdsSettings.advancedJsonPath.notExists()) {
                    call.respondText("{}")
                } else {
                    call.respondFile(Config.cdsSettings.advancedJsonPath.toFile())
                }
            }
            post {
                call.adminApiAction {
                    val text = call.receiveText()
                    try {
                        // check if parsable
                        AdvancedProperties.fromString(text)
                    } catch (e: SerializationException) {
                        throw ApiActionException("Failed to deserialize advanced.json: ${e.message}", e)
                    }
                    Config.cdsSettings.advancedJsonPath.toFile().writeText(text)
                }
            }
        }

        webSocket("/backendLog") { sendFlow(DataBus.loggerFlow) }
        webSocket("/adminActions") { sendFlow(DataBus.adminActionsFlow) }

        route("/media") {
            get {
                run {
                    val mediaDirectoryFile = Config.mediaDirectory.toFile()
                    call.respond(
                        mediaDirectoryFile.walkTopDown()
                            .filter { it.isFile }.map { it.relativeTo(mediaDirectoryFile).path }.toList()
                    )
                }
            }

            post("/upload") {
                call.adminApiAction {
                    val uploadedFileUrls = mutableListOf<String>()
                    val multipart = call.receiveMultipart()
                    multipart.forEachPart { partData ->
                        if (partData is PartData.FileItem) {
                            Files.write(
                                Config.mediaDirectory.resolve(partData.storeName),
                                partData.streamProvider().readBytes()
                            )
                            uploadedFileUrls += partData.storeName
                        }
                    }
                    uploadedFileUrls
                }
            }
        }
    }
    route("/social") {
        setupSocial()
    }
}

private val PartData.FileItem.storeName: String
    get() {
        return this.originalFileName!!.replace("[^\\w.]".toRegex(), "_")
    }
