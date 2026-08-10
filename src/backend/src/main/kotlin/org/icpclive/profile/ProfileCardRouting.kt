package org.icpclive.profile

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.cds.api.toTeamId
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import java.io.File
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

private val logger by getLogger()

/** Profile cards, templates included, are hand-written files; anything bigger is a mistake. */
private const val MAX_FILE_SIZE = 10L * 1024 * 1024

/** Templates are shared between all teams, so their text is cached until the file changes. */
private val templateCache = ConcurrentHashMap<Path, Pair<FileTime, String>>()

/**
 * Resolves [relative] inside [root] and returns the file only if it stays inside the root
 * both lexically and after symlink resolution, and is a regular file. Returns `null`
 * for anything else, including unusable paths and filesystem errors.
 */
private fun resolveInside(root: Path, relative: String): Path? = try {
    val rootPath = root.toAbsolutePath().normalize()
    val file = rootPath.resolve(relative).toAbsolutePath().normalize()
    if (!file.startsWith(rootPath) || !file.isRegularFile()) {
        null
    } else {
        val realFile = file.toRealPath()
        if (realFile.startsWith(rootPath.toRealPath())) realFile else null
    }
} catch (e: InvalidPathException) {
    logger.warning { "Can't resolve profile card path $relative in $root: ${e.message}" }
    null
} catch (e: IOException) {
    logger.warning { "Can't resolve profile card path $relative in $root: ${e.message}" }
    null
} catch (e: SecurityException) {
    logger.warning { "Can't resolve profile card path $relative in $root: ${e.message}" }
    null
}

private fun readLimited(file: Path): String? = try {
    val size = file.fileSize()
    if (size > MAX_FILE_SIZE) {
        logger.warning { "Refusing to read $file: $size bytes is over the $MAX_FILE_SIZE bytes limit" }
        null
    } else {
        file.readText()
    }
} catch (e: IOException) {
    logger.warning { "Failed to read $file: ${e.message}" }
    null
} catch (e: SecurityException) {
    logger.warning { "Failed to read $file: ${e.message}" }
    null
}

private fun loadTemplate(file: Path): String? {
    val modifiedAt = try {
        file.getLastModifiedTime()
    } catch (e: IOException) {
        logger.warning { "Failed to read $file: ${e.message}" }
        return null
    } catch (e: SecurityException) {
        logger.warning { "Failed to read $file: ${e.message}" }
        return null
    }
    val cached = templateCache[file]
    if (cached != null && cached.first == modifiedAt) return cached.second
    val text = readLimited(file) ?: return null
    templateCache[file] = modifiedAt to text
    return text
}

private fun loadSettings(profilesDirectory: Path): ProfileRenderSettings? {
    val file = resolveInside(profilesDirectory, "settings.json") ?: return null
    val text = readLimited(file) ?: return null
    return try {
        settingsJson.decodeFromString(ProfileRenderSettings.serializer(), text).withValidatedColors()
    } catch (e: Exception) {
        logger.warning { "Failed to parse $file: ${e.message}" }
        null
    }
}

private fun loadProfile(profilesDirectory: Path, teamId: String): JsonObject? {
    val file = resolveInside(profilesDirectory.resolve("teams"), "$teamId.json") ?: return null
    val text = readLimited(file) ?: return null
    return try {
        Json.parseToJsonElement(text) as? JsonObject
            ?: run {
                logger.warning { "Failed to parse $file: top level element is not an object" }
                null
            }
    } catch (e: Exception) {
        logger.warning { "Failed to parse $file: ${e.message}" }
        null
    }
}

@OptIn(InefficientContestInfoApi::class)
fun Route.configureProfileCardRouting(
    mediaDirectory: Path,
    profilesDirectory: Path,
    contestInfoProvider: suspend () -> ContestInfo = { DataBus.currentContestInfo() },
) {
    get("{path...}") {
        val relativePath = call.parameters.getAll("path")?.joinToString(File.separator) ?: ""
        val templatePath = resolveInside(mediaDirectory, relativePath)
        if (templatePath == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val teamIdStr = call.request.queryParameters["teamId"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing teamId")
            return@get
        }
        val contestInfo = contestInfoProvider()
        val team = contestInfo.teams[teamIdStr.toTeamId()]
        if (team == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val template = loadTemplate(templatePath)
        if (template == null) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }
        val organization = team.organizationId?.let { contestInfo.organizations[it] }
        val settings = loadSettings(profilesDirectory)
        val profile = loadProfile(profilesDirectory, teamIdStr)
        val roster = extractRoster(team, contestInfo.personsList, settings?.contestType == ContestType.PERSONAL)
        val record = reconcileProfile(profile, roster, team, organization)
        val svg = buildProfileCardSvg(template, record, settings, team.color?.value)
        call.respondBytes(ContentType.Image.SVG) { svg.toByteArray() }
    }
}
