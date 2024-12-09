package org.icpclive.cds.tunning

import io.ktor.http.encodeURLParameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

private val templateRegex = kotlin.text.Regex("\\{(!?[a-z0-9A-Z_.-]*)}")

private fun ContestInfo.getTemplateValue(
    template: String,
    teamId: TeamId,
    extraValues: Map<String, String>,
    isUrl: Boolean
): String {
    val team = teams[teamId] ?: return template
    val org = organizations[team.organizationId]
    val provider = fun(it: String): String? {
        return when {
            it.startsWith("team.") -> {
                when (it.removePrefix("team.")) {
                    "id" -> team.id.value
                    "displayName" -> team.displayName
                    "fullName" -> team.fullName
                    else -> null
                }
            }

            it.startsWith("org.") -> {
                when (it.removePrefix("org.")) {
                    "id" -> org?.id?.value
                    "displayName" -> org?.displayName
                    "fullName" -> org?.fullName
                    else -> null
                }
            }

            it.startsWith("regexes.") -> extraValues[it.removePrefix("regexes.")]
            else -> team.customFields[it]
        }
    }
    return template.replace(templateRegex) {
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

@Serializable
public class TemplateRegexParser(
    public val from: String,
    public val rules: Map<Regex, Map<String, String>>,
)

@Serializable
@SerialName("overrideTeamTemplate")
public data class OverrideTeamTemplate(
    public val regexes: Map<String, TemplateRegexParser> = emptyMap(),
    public val fullName: String? = null,
    public val groups: List<String>? = null,
    public val extraGroups: List<String>? = null,
    public val organizationId: String? = null,
    public val displayName: String? = null,
    public val hashTag: String? = null,
    public val customFields: Map<String, String>? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val color: String? = null,
): DesugarableTuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideTeams(
            info.teamList.associate { teamInfo ->
                val extraCustomValues = buildMap {
                    for ((blockName, parser) in regexes) {
                        val fromValue = info.getTemplateValue(parser.from, teamInfo.id, emptyMap(), false)
                        for ((regex, extras) in parser.rules) {
                            val match = regex.matchEntire(fromValue) ?: continue
                            for ((key, value) in extras) {
                                put("$blockName.$key", regex.replace(fromValue, value))
                            }
                            for ((index, group) in match.groupValues.withIndex()) {
                                put("$blockName.$index", group)
                            }
                            break
                        }
                    }
                }
                fun String.ifMatched() = takeUnless { it.contains(templateRegex) }
                fun String.substituted(isUrl: Boolean) = info.getTemplateValue(this, teamInfo.id, extraCustomValues, isUrl)
                teamInfo.id to TeamInfoOverride(
                    hashTag = hashTag?.substituted(false),
                    fullName = fullName?.substituted(false),
                    displayName = displayName?.substituted(false),
                    groups = groups?.mapNotNull { it.substituted(false).ifMatched()?.toGroupId() },
                    extraGroups = extraGroups?.mapNotNull { it.substituted(false).ifMatched()?.toGroupId() },
                    organizationId = organizationId?.substituted(false)?.ifMatched()?.toOrganizationId(),
                    medias = medias?.mapValues { (_, v) ->
                        v?.let {
                            when (it) {
                                is MediaType.Image -> it.copy(url = it.url.substituted(true))
                                is MediaType.Video -> it.copy(url = it.url.substituted(true))
                                is MediaType.M2tsVideo -> it.copy(url = it.url.substituted(true))
                                is MediaType.HLSVideo -> it.copy(url = it.url.substituted(true))
                                is MediaType.Object -> it.copy(url = it.url.substituted(true))
                                is MediaType.WebRTCProxyConnection -> it.copy(url = it.url.substituted(true))
                                is MediaType.WebRTCGrabberConnection -> it.copy(
                                    url = it.url.substituted(true),
                                    peerName = it.peerName.substituted(false),
                                    credential = it.credential?.substituted(false)
                                )

                                else -> it
                            }
                        }
                    },
                    customFields = customFields?.mapValues { (_, v) -> v.substituted(false) },
                    color = color?.substituted(false)?.let { Color.normalize(it) }
                )
            }
        )
    }
}