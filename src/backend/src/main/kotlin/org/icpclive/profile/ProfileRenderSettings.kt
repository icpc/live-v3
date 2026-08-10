package org.icpclive.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ProfileRenderSettings(
    val contestType: String? = null,
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

private fun String.escapeForSvgBlob() = replace("<", "\\u003c").replace(">", "\\u003e")

internal fun buildProfileCardSvg(
    template: String,
    record: JsonObject,
    settings: ProfileRenderSettings?,
    teamColor: String?,
): String {
    var result = template.replace("{team.json}", record.toString().escapeForSvgBlob())
    if (settings != null) {
        val renderJson = settingsJson.encodeToString(ProfileRenderSettings.serializer(), settings)
        result = result.replace("{render.json}", renderJson.escapeForSvgBlob())
        if (settings.fontColor != null) {
            result = result.replace("{fontColor}", settings.fontColor)
        }
    }
    if (teamColor != null) {
        result = result.replace("{mainColor}", teamColor)
    }
    return result
}
