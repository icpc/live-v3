package org.icpclive.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

private val colorRegex = Regex("^#[0-9a-fA-F]{3,8}$")

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

private fun String.escapeForSvgBlob() = replace("<", "\\u003c").replace(">", "\\u003e")

internal fun buildProfileCardSvg(
    template: String,
    record: JsonObject,
    settings: ProfileRenderSettings?,
    teamColor: String?,
): String {
    var result = template
    if (teamColor != null) {
        result = result.replace("{mainColor}", teamColor)
    }
    if (settings != null) {
        if (settings.fontColor != null) {
            result = result.replace("{fontColor}", settings.fontColor)
        }
        val renderJson = settingsJson.encodeToString(ProfileRenderSettings.serializer(), settings)
        result = result.replace("{render.json}", renderJson.escapeForSvgBlob())
    }
    // The team record is the largest chunk of externally provided text, so it is substituted
    // last: no other token can be corrupted by data that happens to contain a token literal.
    return result.replace("{team.json}", record.toString().escapeForSvgBlob())
}
