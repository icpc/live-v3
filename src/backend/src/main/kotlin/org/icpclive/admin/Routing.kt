package org.icpclive.admin

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.datetime.Instant
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.icpclive.Config
import org.icpclive.api.TeamViewPosition
import org.icpclive.api.WidgetUsageStatisticsEntry
import org.icpclive.cds.api.Color
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.ScoreMergeMode
import org.icpclive.cds.api.startTime
import org.icpclive.cds.api.toGroupId
import org.icpclive.cds.api.toOrganizationId
import org.icpclive.cds.tunning.*
import org.icpclive.data.*
import org.icpclive.util.sendFlow
import kotlin.io.path.notExists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val json1 = Json {
    prettyPrint = true
}

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
        fun Route.setupTeamViews(name:String, vararg positions: TeamViewPosition) {
            route("/$name") {
                setupControllerGroup(positions.associate { it.name to Controllers.teamView(it) })
                positions.forEach { position ->
                    route("/${position.name}") { setupController(Controllers.teamView(position)) }
                }
                get("/teams") { call.respond(getTeams()) }
                get("/usage_stats") {
                    val entry = Controllers.getWidgetStats().entries["teamview"] as? WidgetUsageStatisticsEntry.PerTeam
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
                    ).toRulesList())
                }
            }
        }

        route("/advancedJson") {
            get("/rulePreview") {
                val type = call.request.queryParameters["type"]
                val fields = call.request.queryParameters["fields"]?.split(",")?.toSet() ?: emptySet()
                if (type == "all") {
                    call.respond(DataBus.currentContestInfo().toAdvancedProperties(fields).toRulesList())
                    return@get
                }
                val rule: TuningRule = when (type) {
                    "override_all_teams" if fields.singleOrNull() == "displayName" -> OverrideTeamDisplayNames(
                        DataBus.currentContestInfo().teams.mapValues { it.value.displayName }
                    )
                    "override_all_teams" -> OverrideTeams(
                        DataBus.currentContestInfo().teams.mapValues { (_, it) ->
                            TeamInfoOverride(
                                fullName = it.fullName.takeIf { fields.contains("fullName") },
                                displayName = it.displayName.takeIf { fields.contains("displayName") },
                                groups = it.groups.takeIf { fields.contains("groups") },
                                organizationId = (it.organizationId ?: "".toOrganizationId()).takeIf { fields.contains("organizationId") },
                                hashTag = it.hashTag.orEmpty().takeIf { fields.contains("hashTag") },
                                medias = it.medias.takeIf { fields.contains("medias") },
                                customFields = it.customFields.takeIf { fields.contains("customFields") },
                                isHidden = it.isHidden.takeIf { fields.contains("isHidden") },
                                isOutOfContest = it.isOutOfContest.takeIf { fields.contains("isOutOfContest") },
                                color = (it.color ?: Color.normalize("#000000")).takeIf { fields.contains("color") },
                            )
                        }
                    )
                    "override_all_groups" -> OverrideGroups(
                        DataBus.currentContestInfo().groups.mapValues { (_, it) ->
                            GroupInfoOverride(
                                displayName = it.displayName.takeIf { fields.contains("displayName") },
                                isHidden = it.isHidden.takeIf { fields.contains("isHidden") },
                                isOutOfContest = it.isOutOfContest.takeIf { fields.contains("isOutOfContest") },
                            )
                        }
                    )
                    "override_all_organizations" if fields.singleOrNull() == "displayName" -> OverrideOrganizationDisplayNames(
                        DataBus.currentContestInfo().organizations.mapValues { it.value.displayName }
                    )
                    "override_all_organizations" -> OverrideOrganizations(
                        DataBus.currentContestInfo().organizations.mapValues { (_, it) ->
                            OrganizationInfoOverride(
                                displayName = it.displayName.takeIf { fields.contains("displayName") },
                                fullName = it.fullName.takeIf { fields.contains("fullName") },
                                logo = (it.logo ?: MediaType.Image("")).takeIf { fields.contains("logo") },
                            )
                        }
                    )
                    "override_all_problems" if fields.singleOrNull() == "color" -> OverrideProblemColors(
                        DataBus.currentContestInfo().problems.mapValues { it.value.color ?: Color.normalize("#000000") }
                    )
                    "override_all_problems" -> OverrideProblems(
                        DataBus.currentContestInfo().problems.mapValues { (_, it) ->
                            ProblemInfoOverride(
                                displayName = it.displayName.takeIf { fields.contains("displayName") },
                                fullName = it.fullName.takeIf { fields.contains("fullName") },
                                color = (it.color ?: Color.normalize("#000000")).takeIf { fields.contains("color")},
                                unsolvedColor = (it.unsolvedColor ?: Color.normalize("#000000")).takeIf { fields.contains("unsolvedColor") },
                                ordinal = it.ordinal.takeIf { fields.contains("ordinal") },
                                minScore = (it.minScore ?: 0.0).takeIf { fields.contains("minScore") },
                                maxScore = (it.maxScore ?: 0.0).takeIf { fields.contains("maxScore") },
                                scoreMergeMode = (it.scoreMergeMode ?: ScoreMergeMode.SUM).takeIf { fields.contains("scoreMergeMode") },
                                isHidden = it.isHidden.takeIf { fields.contains("isHidden") },
                            )
                        }
                    )
                    "override_times" -> {
                        val info = DataBus.currentContestInfo()
                        OverrideTimes(
                            startTime = (info.startTime ?: Instant.fromEpochMilliseconds(0)).takeIf { fields.contains("startTime") },
                            contestLength = info.contestLength.takeIf { fields.contains("contestLength") },
                            freezeTime = (info.freezeTime ?: 1.hours).takeIf { fields.contains("freezeTime") },
                            holdTime = ((info.status as? ContestStatus.BEFORE)?.holdTime ?: Duration.ZERO).takeIf { fields.contains("holdTime") }
                        )
                    }
                    "add_custom_value" -> {
                        val name = call.queryParameters["name"] ?: ""
                        AddCustomValue(
                            name = name,
                            rules = DataBus.currentContestInfo().teams.mapValues { (_, it) -> it.customFields[name] ?: "" }
                        )
                    }
                    "add_group_by_regex" -> {
                        val name = call.queryParameters["name"] ?: ""
                        AddGroupIfMatches(
                            id = name.toGroupId(),
                            from = call.queryParameters["from"] ?: "",
                            rule = Regex(call.queryParameters["regex"] ?: "")
                        )
                    }
                    else -> return@get call.respondText("Unknown type $type")
                }
                call.respond(
                    mapOf(
                        "preview" to Json.encodeToJsonElement(rule),
                        "expanded" to (rule as? DesugarableTuningRule)?.desugar(DataBus.currentContestInfo()).let { Json.encodeToJsonElement(it) }
                    )
                )
            }
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
                        TuningRule.listFromString(text)
                    } catch (e: SerializationException) {
                        throw ApiActionException("Failed to deserialize advanced.json: ${e.message}", e)
                    }
                    Config.cdsSettings.advancedJsonPath.toFile().writeText(text)
                }
            }
        }


        get("/contestInfo") {
            run {
                call.respondText(contentType = ContentType.Application.Json) {
                    Json.encodeToString(DataBus.currentContestInfo())
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
            call.respond(Controllers.getWidgetStats())
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
