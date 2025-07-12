package org.icpclive.cds.tunning

import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.logger
import org.icpclive.cds.util.runCatchingIfNotCancellation

private val templateRegex = Regex("\\{(!?[a-z0-9A-Z_.-]*)}")

private fun String.getTemplateValue(
    team: TeamInfo?,
    org: OrganizationInfo?,
    extraValues: Map<String, String>,
    isUrl: Boolean
): String {
    val provider = fun(it: String): String? {
        return when {
            it.startsWith("team.") -> {
                if (team == null) {
                    null
                } else {
                    when (it.removePrefix("team.")) {
                        "id" -> team.id.value
                        "displayName" -> team.displayName
                        "fullName" -> team.fullName
                        "hashTag" -> team.hashTag
                        "color" -> team.color.toString()
                        else -> null
                    }
                }
            }

            it.startsWith("org.") -> {
                if (org == null) {
                    null
                } else {
                    when (it.removePrefix("org.")) {
                        "id" -> org.id.value
                        "displayName" -> org.displayName
                        "fullName" -> org.fullName
                        else -> null
                    }
                }
            }

            it.startsWith("regexes.") -> extraValues[it.removePrefix("regexes.")]
            else -> team?.customFields[it]
        }
    }
    return replace(templateRegex) {
        val str = it.groups[1]!!.value
        if (isUrl) {
            if (str.startsWith("!")) {
                provider(str.removePrefix("!"))
            } else {
                provider(str)?.encodeURLParameter()
            }
        } else {
            provider(str)
        } ?: it.value
    }
}


internal interface TemplateSubstitutor {
    fun String.substituteUrl(): String
    fun String.substituteRegular(): String

    fun MediaType.substitute(): MediaType {
        return when (this) {
            is MediaType.Image -> copy(url = url.substituteUrl())
            is MediaType.Video -> copy(url = url.substituteUrl())
            is MediaType.M2tsVideo -> copy(url = url.substituteUrl())
            is MediaType.HLSVideo -> copy(url = url.substituteUrl())
            is MediaType.Object -> copy(url = url.substituteUrl())
            is MediaType.WebRTCProxyConnection -> copy(url = this.url.substituteUrl())
            is MediaType.WebRTCGrabberConnection -> copy(
                url = url.substituteUrl(),
                peerName = peerName.substituteRegular(),
                credential = credential?.substituteRegular()
            )

            else -> this
        }

    }
}

internal fun getSubstitutor(regexes: Map<String, TemplateRegexParser>, teamInfo: TeamInfo?, orgInfo: OrganizationInfo?): TemplateSubstitutor {
    val customValues = buildMap {
        fun <T> Result<T>.getOrNullAndWarn(regex: Regex, value: String, replacement: String? = null): T? {
            exceptionOrNull()?.let {
                logger(OverrideTeamTemplate::class).error(it) {
                    "Problems during executing regular expression `${regex}` on value `$value` with replacement `$replacement`"
                }
            }
            return getOrNull()
        }
        for ((blockName, parser) in regexes) {
            val fromValue = parser.from.getTemplateValue(teamInfo, orgInfo, this, false)
            for ((regex, extras) in parser.rules) {
                val match = runCatchingIfNotCancellation {
                    regex.matchEntire(fromValue)
                }.getOrNullAndWarn(regex, fromValue, null) ?: continue
                for ((index, group) in match.groupValues.withIndex().drop(1)) {
                    put("$blockName.$index", group)
                }
                for ((key, value) in extras) {
                    put("$blockName.$key", runCatchingIfNotCancellation {
                        regex.replace(fromValue, value)
                    }.getOrNullAndWarn(regex, fromValue, value) ?: continue)
                }
                break
            }
        }
    }
    return object : TemplateSubstitutor {
        override fun String.substituteRegular(): String {
            return this.getTemplateValue(teamInfo, orgInfo, customValues, false)
        }

        override fun String.substituteUrl(): String {
            return this.getTemplateValue(teamInfo, orgInfo, customValues, true)
        }
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
