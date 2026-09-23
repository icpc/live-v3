package org.icpclive.admin

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import org.icpclive.Config
import org.icpclive.api.TeamViewPosition
import org.icpclive.api.WidgetUsageStatisticsEntry
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.tunning.toRulesList
import org.icpclive.data.*
import org.icpclive.server.ApiActionException
import org.icpclive.server.adminApiAction
import org.icpclive.server.configureDefaultConfigRouting

context(controllers: Controllers, dataBus: DataBus)
fun Route.configureAdminApiRouting() {
    authenticate("admin-api-auth") {
        route("/queue") { setupController(controllers.queue) }
        route("/statistics") { setupController(controllers.statistics) }
        route("/ticker") { setupController(controllers.ticker) }
        route("/scoreboard") {
            setupController(controllers.scoreboard)
            get("/regions") {
                call.respond(getRegions())
            }
        }
        fun Route.setupTeamViews(name:String, vararg positions: TeamViewPosition) {
            route("/$name") {
                setupControllerGroup(positions.associate { it.name to controllers.teamView(it) })
                positions.forEach { position ->
                    route("/${position.name}") { setupController(controllers.teamView(position)) }
                }
                get("/teams") { call.respond(getTeams()) }
                get("/usage_stats") {
                    val entry = controllers.getWidgetStats().entries["teamView"] as? WidgetUsageStatisticsEntry.PerTeam
                    call.respond(entry ?: WidgetUsageStatisticsEntry.PerTeam(emptyMap()))
                }
            }
        }
        setupTeamViews("teamView", TeamViewPosition.SINGLE)
        setupTeamViews("teamPVP", TeamViewPosition.PVP_TOP, TeamViewPosition.PVP_BOTTOM)
        setupTeamViews(
            "splitScreen",
            TeamViewPosition.TOP_LEFT,
            TeamViewPosition.TOP_RIGHT,
            TeamViewPosition.BOTTOM_LEFT,
            TeamViewPosition.BOTTOM_RIGHT
        )
        route("/fullScreenClock") { setupController(controllers.fullScreenClock) }
        route("/teamLocator") { setupController(controllers.locator) }


        route("/advertisement") { setupController(controllers.advertisement) }
        route("/picture") { setupController(controllers.picture) }
        route("/title") {
            setupController(controllers.title)
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
        route("/tickerMessage") { setupController(controllers.tickerMessage) }
        route("/analytics") {
            setupAnalytics()
        }

        route("/teamSpotlight") { setupSpotlight() }

        route("/users") { setupUserRouting(controllers.userController) }
        get("/advancedJsonPreview") {
            val formatter = Json {
                prettyPrint = true
                encodeDefaults = true
                explicitNulls = true
            }
            run {
                call.respondText(contentType = ContentType.Application.Json) {
                    val fields = call.request.queryParameters["fields"]?.split(",")?.map { it.lowercase() }?.toSet() ?: emptySet()
                    val rulesList = dataBus.currentContestInfo().toRulesList()
                    val serializer = object : JsonTransformingSerializer<TuningRule>(TuningRule.serializer()) {
                        override fun transformSerialize(element: JsonElement): JsonElement {
                            if (element !is JsonObject) return element
                            if ("all" in fields) return element
                            val prefix = element["type"]?.jsonPrimitive?.content?.removePrefix("override")?.lowercase() ?: return element
                            if (fields.none { it.startsWith(prefix) }) return JsonNull
                            if ("$prefix.all" in fields || prefix in fields) return element
                            val filtered = if ("rules" !in element) {
                                JsonObject(element.filterKeys { it == "type" || "$prefix.${it.lowercase()}" in fields })
                            } else {
                                JsonObject(element.mapValues { (k, v) ->
                                    if (k == "rules" && v is JsonObject) {
                                        val filteredRules = v.mapValues { (_, value) ->
                                            if (value is JsonObject) {
                                                val sub = value.filterKeys { "$prefix.${it.lowercase()}" in fields }
                                                if (sub.isNotEmpty()) JsonObject(sub) else JsonNull
                                            } else {
                                                value
                                            }
                                        }.filterValues { it !is JsonNull }
                                        if (filteredRules.isNotEmpty()) {
                                            JsonObject(filteredRules)
                                        } else {
                                            JsonNull
                                        }
                                    } else {
                                        v
                                    }
                                }.filterValues { it !is JsonNull })
                            }
                            if (filtered.keys.any { it != "type" }) return filtered
                            return JsonNull
                        }
                    }
                    val listSerializer = ListSerializer(serializer)
                    val list = formatter.encodeToJsonElement(listSerializer, rulesList)
                    val filtered = JsonArray((list as JsonArray).filter { it !is JsonNull })
                    formatter.encodeToString(filtered)
                }
            }
        }

        configureDefaultConfigRouting(
            Config.cdsSettings.settingsPath,
            Config.cdsSettings.advancedJsonPath,
            Config.visualConfigFile,
            Config.cdsSettings.customFieldsCsvPath,
            Config.cdsSettings.orgCustomFieldsCsvPath,
            { dataBus.currentContestInfoFlow() },
            onSettingsReload = {
                // Fail the request on a file we can't load, instead of letting the fresh
                // application die on it later: that would take the whole process down.
                try {
                    withContext(Dispatchers.IO) { Config.cdsSettings.parseSettings() }
                } catch (e: Exception) {
                    throw ApiActionException("Settings file is saved, but can't be loaded: ${e.message}", e)
                }
                Config.requestReload()
            },
        )

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
                            val file = Config.mediaDirectory.resolve(partData.storeName).toFile()
                            partData.provider().copyAndClose(file.writeChannel())
                            uploadedFileUrls += partData.storeName
                        }
                    }
                    uploadedFileUrls
                }
            }
        }
        get("/usage_stats") {
            call.respond(controllers.getWidgetStats())
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
