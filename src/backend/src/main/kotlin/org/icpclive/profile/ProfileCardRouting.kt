package org.icpclive.profile

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.cds.api.toTeamId
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private val logger by getLogger()

private fun loadSettings(profilesDirectory: Path): ProfileRenderSettings? {
    val file = profilesDirectory.resolve("settings.json")
    if (!file.isRegularFile()) return null
    return try {
        settingsJson.decodeFromString(ProfileRenderSettings.serializer(), file.readText())
    } catch (e: Exception) {
        logger.warning { "Failed to parse $file: ${e.message}" }
        null
    }
}

private fun loadProfile(profilesDirectory: Path, teamId: String): JsonObject? {
    val profilesRoot = profilesDirectory.toAbsolutePath().normalize()
    val file = profilesRoot.resolve("$teamId.json").toAbsolutePath().normalize()
    if (!file.startsWith(profilesRoot) || !file.isRegularFile()) return null
    return try {
        Json.parseToJsonElement(file.readText()) as? JsonObject
    } catch (e: Exception) {
        logger.warning { "Failed to parse $file: ${e.message}" }
        null
    }
}

@OptIn(InefficientContestInfoApi::class)
fun Route.configureProfileCardRouting(mediaDirectory: Path, profilesDirectory: Path) {
    get("{path...}") {
        val relativePath = call.parameters.getAll("path")?.joinToString(File.separator) ?: ""
        val mediaRoot = mediaDirectory.toAbsolutePath().normalize()
        val templatePath = mediaRoot.resolve(relativePath).toAbsolutePath().normalize()
        if (!templatePath.startsWith(mediaRoot) || !templatePath.isRegularFile()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val teamIdStr = call.request.queryParameters["teamId"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing teamId")
            return@get
        }
        val contestInfo = DataBus.currentContestInfo()
        val team = contestInfo.teams[teamIdStr.toTeamId()]
        if (team == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val organization = team.organizationId?.let { contestInfo.organizations[it] }
        val settings = loadSettings(profilesDirectory)
        val profile = loadProfile(profilesDirectory, teamIdStr)
        val roster = extractRoster(team, contestInfo.personsList, settings?.contestType == "Personal")
        val record = reconcileProfile(profile, roster, team, organization)
        val svg = buildProfileCardSvg(templatePath.readText(), record, settings, team.color?.value)
        call.respondBytes(ContentType.Image.SVG) { svg.toByteArray() }
    }
}
