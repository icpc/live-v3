package org.icpclive.cds.tunning

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.icpclive.cds.api.*
import org.icpclive.cds.util.logger
import org.icpclive.cds.util.runCatchingIfNotCancellation


private val templateRegex = Regex("\\{(!?[a-z0-9A-Z_.-]*)}")

public interface TemplateSubstitutor {
    public fun substitute(data: String, sanitizer: (String) -> String): String
}

public fun TemplateSubstitutor.substituteUrlSafe(data: String): String = substitute(data) { it.encodeURLParameter() }

public fun TemplateSubstitutor.substitute(type: MediaType): MediaType {
    return when (type) {
        is MediaType.Image -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.Video -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.M2tsVideo -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.HLSVideo -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.Object -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.WebRTCProxyConnection -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.WebRTCGrabberConnection -> type.copy(
            url = substituteUrlSafe(type.url),
            peerName = substituteRaw(type.peerName),
            credential = type.credential?.let { substituteRaw(it) }
        )
        is MediaType.Audio -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.Text -> type.copy(url = substituteUrlSafe(type.url))
        is MediaType.ZipArchive -> type.copy(url = substituteUrlSafe(type.url))
    }
}
public fun TemplateSubstitutor.substituteRaw(data: String): String = substitute(data) { it }

@JvmName("substituteRawNullable")
public fun TemplateSubstitutor.substituteRaw(data: String?): String? = data?.let { substituteRaw(it) }
@JvmName("substituteUrlNullable")
public fun TemplateSubstitutor.substituteUrlSafe(data: String?): String? = data?.let { substituteUrlSafe(it) }
@JvmName("substituteNullable")
public fun TemplateSubstitutor.substitute(data: MediaType?): MediaType? = data?.let { substitute(it) }


internal class TemplateSubstitutorImpl(private val data: JsonObject) : TemplateSubstitutor {
    private fun getByKey(key: String): String? {
        var cur = data
        val parts = key.split(".")
        for (i in parts.dropLast(1)) {
            cur = cur[i] as? JsonObject ?: return null
        }
        val result = cur[parts.last()] ?: return null
        return when (result) {
            is JsonArray, is JsonObject -> Json.encodeToString(result)
            is JsonPrimitive -> result.content
            JsonNull -> null
        }
    }


    private fun String.getTemplateValue(
        sanitizer: (String) -> String
    ): String {
        return replace(templateRegex) {
            val str = it.groups[1]!!.value
            if (str.startsWith("!")) {
                getByKey(str.removePrefix("!"))
            } else {
                getByKey(str)?.let(sanitizer)
            } ?: it.value
        }
    }


    override fun substitute(data: String, sanitizer: (String) -> String): String = data.getTemplateValue(sanitizer)
}

internal fun getSubstitutor(regexes: Map<String, TemplateRegexParser>, teamInfo: TeamInfo?, orgInfo: OrganizationInfo?): TemplateSubstitutor {
    return buildTemplateSubstitutor {
        teamInfo?.let(::addTeam)
        orgInfo?.let(::addOrganization)
        regexes.forEach { (name, parser) -> addRegex(name, parser) }
    }
}

/**
 * @see [OverrideTeamTemplate] for details
 */
@Serializable
public class TemplateRegexParser(
    public val from: String,
    public val rules: Map<Regex, Map<String, String>>,
)

internal fun String.hasNoUnsubstitutedRegex() = takeUnless { it.contains(templateRegex) }

public class TemplateSubstitutionBuilder @PublishedApi internal constructor() {
    private var map = mutableMapOf<String, JsonElement>()

    public fun addJson(key: String, value: JsonElement) {
        map[key] = value
    }
    public fun addTeam(info: TeamInfo, name: String = "team", withFlatCustomFields: Boolean = true) {
        val teamJson = Json.encodeToJsonElement(info) as JsonObject
        addJson(name, buildJsonObject {
            for ((k, v) in teamJson) {
                put(k, v)
                if (k == "name") put("fullName", v)
                if (k == "shortName") put("displayName", v)
            }
        })
        if (withFlatCustomFields) {
            for ((k, v) in (teamJson["customFields"] as? JsonObject).orEmpty()) {
                addJson(k, v)
            }
        }
    }
    public fun addOrganization(info: OrganizationInfo, name: String = "org") {
        addJson(name, Json.encodeToJsonElement(info) as JsonObject)
    }
    public fun addRegex(groupName: String, parser: TemplateRegexParser, name: String = "regexes") : TemplateSubstitutionBuilder = apply {
        fun <T> Result<T>.getOrNullAndWarn(regex: Regex, value: String, replacement: String? = null): T? {
            exceptionOrNull()?.let {
                logger(OverrideTeamTemplate::class).error(it) {
                    "Problems during executing regular expression `${regex}` on value `$value` with replacement `$replacement`"
                }
            }
            return getOrNull()
        }

        val fromValue = with (build()) { substituteRaw(parser.from) }
        val result = buildMap {
            for ((regex, extras) in parser.rules) {
                val match = runCatchingIfNotCancellation {
                    regex.matchEntire(fromValue)
                }.getOrNullAndWarn(regex, fromValue, null) ?: continue
                for ((index, group) in match.groupValues.withIndex().drop(1)) {
                    put("$index", group)
                }
                for ((key, value) in extras) {
                    put(key, runCatchingIfNotCancellation {
                        regex.replace(fromValue, value)
                    }.getOrNullAndWarn(regex, fromValue, value) ?: continue)
                }
                break
            }
        }
        val oldValue = (map[name] as? JsonObject)
        val newValue = mapOf(groupName to JsonObject(result.mapValues { JsonPrimitive(it.value) })) + oldValue.orEmpty()
        addJson(name, JsonObject(newValue))
    }

    @PublishedApi
    internal fun build(): TemplateSubstitutor = TemplateSubstitutorImpl(JsonObject(map))
}

public inline fun buildTemplateSubstitutor(block: TemplateSubstitutionBuilder.() -> Unit): TemplateSubstitutor = TemplateSubstitutionBuilder().apply(block).build()