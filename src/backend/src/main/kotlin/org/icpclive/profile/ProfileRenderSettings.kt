package org.icpclive.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.icpclive.cds.util.getLogger

private val logger by getLogger()

@Serializable
internal enum class ContestType {
    @SerialName("ICPC")
    ICPC,

    @SerialName("Team")
    TEAM,

    @SerialName("Personal")
    PERSONAL,
}

@Serializable
internal data class ProfileRenderSettings(
    val contestType: ContestType? = null,
    val hideHashtag: Boolean? = null,
    val hideSite: Boolean? = null,
    val finals: Finals? = null,
    val fontColor: String? = null,
    val mainColor: String? = null,
    val logo: String? = null,
    val logoExtension: String? = null,
    val background: String? = null,
) {
    @Serializable
    internal data class Finals(
        val include: Boolean? = null,
        val includeEmpty: Boolean? = null,
    )
}

internal val settingsJson = Json {
    explicitNulls = false
    encodeDefaults = false
    ignoreUnknownKeys = true
}

/** Exactly the hex-color shapes CSS accepts: RGB, RGBA, RRGGBB, RRGGBBAA. */
private val colorRegex = Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")

private fun validColorOrNull(value: String?, name: String): String? = when {
    value == null -> null
    colorRegex.matches(value) -> value
    else -> {
        logger.warning { "Ignoring invalid profile card $name in settings.json: '$value'" }
        null
    }
}

/**
 * Drops `fontColor`/`mainColor` values that are not plain hex colors. Those values end up
 * inside the template's `<style>` block verbatim, so anything else must never be substituted.
 */
internal fun ProfileRenderSettings.withValidatedColors(): ProfileRenderSettings = copy(
    fontColor = validColorOrNull(fontColor, "fontColor"),
    mainColor = validColorOrNull(mainColor, "mainColor"),
)

private val rawColorKeys = listOf("fontColor", "mainColor")

/**
 * The raw-settings equivalent of [withValidatedColors]: removes `fontColor`/`mainColor` keys
 * from the operator's unparsed settings.json object when their value is not a plain hex color,
 * but otherwise leaves the object untouched -- including keys the typed [ProfileRenderSettings]
 * model doesn't know about, which is the whole point of keeping the raw object around instead of
 * re-encoding the typed view for `{render.json}`.
 */
internal fun sanitizeRawSettings(raw: JsonObject): JsonObject {
    val invalidKeys = rawColorKeys.filter { key ->
        val element = raw[key] ?: return@filter false
        val value = (element as? JsonPrimitive)?.takeIf { it.isString }?.content
        val invalid = value == null || !colorRegex.matches(value)
        if (invalid) logger.warning { "Ignoring invalid profile card $key in settings.json: '$element'" }
        invalid
    }
    return if (invalidKeys.isEmpty()) raw else JsonObject(raw.filterKeys { it !in invalidKeys })
}

private fun String.escapeForSvgBlob() = replace("<", "\\u003c").replace(">", "\\u003e")

/**
 * [rawSettings] is the operator's settings.json content -- already sanitized of invalid colors
 * by the caller -- substituted into `{render.json}` verbatim so that keys unknown to the typed
 * model still reach the template. [settings] is the typed, validated view of the same document,
 * used only for backend decisions that need a strongly-typed value: the `{fontColor}` token here,
 * and `contestType` upstream (deciding whether to build a one-person roster).
 */
internal fun buildProfileCardSvg(
    template: String,
    record: JsonObject,
    rawSettings: JsonObject?,
    settings: ProfileRenderSettings?,
    teamColor: String?,
): String {
    var result = template
    if (teamColor != null) {
        result = result.replace("{mainColor}", teamColor)
    }
    if (settings?.fontColor != null) {
        result = result.replace("{fontColor}", settings.fontColor)
    }
    if (rawSettings != null) {
        result = result.replace("{render.json}", rawSettings.toString().escapeForSvgBlob())
    }
    // The team record is the largest chunk of externally provided text, so it is substituted
    // last: no other token can be corrupted by data that happens to contain a token literal.
    return result.replace("{team.json}", record.toString().escapeForSvgBlob())
}
