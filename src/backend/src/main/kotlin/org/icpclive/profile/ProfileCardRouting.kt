package org.icpclive.profile

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.InefficientContestInfoApi
import org.icpclive.cds.api.toTeamId
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
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

/** settings.json and per-team profile files are small, hand-written JSON; keep the limit tight. */
private const val MAX_JSON_FILE_SIZE = 1L * 1024 * 1024

/**
 * Only hand-written templates and settings.json are worth caching; the shipped templates are a
 * few KiB. The media directory also holds team photos/videos reachable through this same route,
 * and those must never be pinned in memory just because someone requested them.
 */
private const val MAX_CACHEABLE_FILE_SIZE = 1L * 1024 * 1024

/**
 * Safety valve in case many distinct files happen to be under [MAX_CACHEABLE_FILE_SIZE]: the
 * working set is normally one or two templates plus settings.json, so hitting this bound clears
 * the map instead of growing it further, rather than trying to be clever about eviction order.
 */
private const val MAX_CACHE_ENTRIES = 64

/** Templates and settings.json are shared between all teams, so their text is cached until the file changes. */
private data class CacheKey(val modifiedAt: FileTime, val size: Long)

private val templateCache = ConcurrentHashMap<Path, Pair<CacheKey, String>>()

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

private fun readLimited(file: Path, maxSize: Long = MAX_FILE_SIZE): String? = try {
    val size = file.fileSize()
    if (size > maxSize) {
        logger.warning { "Refusing to read $file: $size bytes is over the $maxSize bytes limit" }
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

/** Shared by [loadTemplate] and settings.json loading: read [file], caching small files by (mtime, size). */
private fun loadCachedText(file: Path, maxSize: Long): String? {
    val key = try {
        CacheKey(file.getLastModifiedTime(), file.fileSize())
    } catch (e: IOException) {
        logger.warning { "Failed to read $file: ${e.message}" }
        return null
    } catch (e: SecurityException) {
        logger.warning { "Failed to read $file: ${e.message}" }
        return null
    }
    val cached = templateCache[file]
    if (cached != null && cached.first == key) return cached.second
    val text = readLimited(file, maxSize) ?: return null
    if (key.size <= MAX_CACHEABLE_FILE_SIZE) {
        if (templateCache.size >= MAX_CACHE_ENTRIES) templateCache.clear()
        // The stat above and the read inside readLimited() are not atomic with each other, so
        // under concurrent requests one thread's (key, text) pair can be built from a file that
        // changed mid-read while another thread's fresher pair races it to the map -- a torn
        // window between metadata and content. `merge` makes the map write itself atomic and
        // keeps whichever entry carries the newer mtime, so a stale read can never clobber a
        // fresher one; any residual staleness self-heals the next time the file's mtime changes.
        // The size check just above is similarly only a soft cap under concurrency (two threads
        // can each observe size < MAX_CACHE_ENTRIES and both insert): that's fine, clear-on-
        // overflow is a deliberately cheap approximate reset rather than a hard limit.
        templateCache.merge(file, key to text) { old, new ->
            if (old.first.modifiedAt >= new.first.modifiedAt) old else new
        }
    }
    return text
}

private fun loadTemplate(file: Path): String? = loadCachedText(file, MAX_FILE_SIZE)

/**
 * Loads settings.json, returning both the raw parsed object (sanitized of invalid colors, for
 * verbatim `{render.json}` substitution) and the typed, color-validated view (for backend
 * decisions such as `contestType`/`fontColor`). Returns `null` if the file is absent, oversized,
 * unreadable, or fails to parse.
 */
private fun loadSettings(profilesDirectory: Path): Pair<JsonObject, ProfileRenderSettings>? {
    val file = resolveInside(profilesDirectory, "settings.json") ?: return null
    val text = loadCachedText(file, MAX_JSON_FILE_SIZE) ?: return null
    return try {
        val raw = Json.parseToJsonElement(text) as? JsonObject ?: run {
            logger.warning { "Failed to parse $file: top level element is not an object" }
            return null
        }
        val typed = settingsJson.decodeFromJsonElement(ProfileRenderSettings.serializer(), raw).withValidatedColors()
        sanitizeRawSettings(raw) to typed
    } catch (e: Exception) {
        logger.warning { "Failed to parse $file: ${e.message}" }
        null
    }
}

private fun loadProfile(profilesDirectory: Path, teamId: String): JsonObject? {
    // A team id is a single path segment; reject anything that could turn "teams/<id>.json" into
    // a path pointing elsewhere in the profiles directory (e.g. "../settings") before resolving it.
    if ('/' in teamId || '\\' in teamId) return null
    val file = resolveInside(profilesDirectory.resolve("teams"), "$teamId.json") ?: return null
    val text = readLimited(file, MAX_JSON_FILE_SIZE) ?: return null
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
        val teamIdStr = call.request.queryParameters["teamId"]
        if (teamIdStr.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing teamId")
            return@get
        }
        val contestInfo = contestInfoProvider()
        val team = contestInfo.teams[teamIdStr.toTeamId()]
        if (team == null) {
            call.respond(HttpStatusCode.NotFound, "Unknown team")
            return@get
        }
        val relativePath = call.parameters.getAll("path")?.joinToString("/") ?: ""
        // Template/settings/profile resolution below is all blocking filesystem I/O; keep it off
        // the dispatcher the route otherwise runs requests on.
        val svg = withContext(Dispatchers.IO) {
            val templatePath = resolveInside(mediaDirectory, relativePath)
            val template = templatePath?.let { loadTemplate(it) } ?: return@withContext null
            val organization = team.organizationId?.let { contestInfo.organizations[it] }
            val loadedSettings = loadSettings(profilesDirectory)
            val rawSettings = loadedSettings?.first
            val settings = loadedSettings?.second
            val profile = loadProfile(profilesDirectory, teamIdStr)
            val roster = extractRoster(team, contestInfo.personsList, settings?.contestType == ContestType.PERSONAL)
            val record = reconcileProfile(profile, roster, team, organization)
            buildProfileCardSvg(template, record, rawSettings, settings, team.color?.value)
        }
        if (svg == null) {
            call.respond(HttpStatusCode.NotFound, "Template not found")
            return@get
        }
        call.respondBytes(ContentType.Image.SVG) { svg.toByteArray() }
    }
}
