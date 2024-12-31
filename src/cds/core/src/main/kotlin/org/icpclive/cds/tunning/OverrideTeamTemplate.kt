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
                    "hashTag" -> team.hashTag
                    "color" -> team.color.toString()
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

/**
 * This is a rule, allowing to update many teams in the same way.
 *
 * Each field is transformed to the corresponding field of [OverrideTeams.Override].
 *
 * All string values are used after substitution of templates of form `{variableName}` to corresponding values.
 * The following variable names are supported:
 * * `{team.id}`, `{team.fullName}`, `{team.displayName}`, `{team.hashTag}`, `{team.color}` - corresponding property of a team.
 *     Note that color would be normalized to 8-digit rgba value, not one, which was set initially.
 * * `{org.id}`, `{org.displayName}, `{org.fullName}` - corresponding property of team's organization.
 * * `{regexes.group.value}` - values parsed by [regexes] field of current rule, see later for details
 * * `{customFieldName} - value from team [TeamInfo.customFields]
 *
 * The operation is atomic, all updates happen at the same time. So substitution can't use value set by this operation.
 * If you need this, you can add several instances of the rule.
 *
 * [regexes] map allow you to parse some data from one of the values.
 * This is a list of named values groups. Each group is parsed from a single value, by the first regex matched in the group.
 *
 * The group name is used as part of variable name for substitution and not important for anything else.
 *
 * [RegexParser.from] is a string to parse with regex. It can use the same substitutions as other strings in that object, except for not-yet-computed regexes.
 *
 * [RegexParser.rules] is map, keys of which defines a set of regexes to match against, and values describes how to use matched regex.
 *
 * The regexes should follow [java.util.regex.Pattern] rules. Keys in the value map are just names for variables, they don't affect anything else.
 * Values of the value map are replacing rules, interpreted as if [java.util.regex.Matcher.replaceAll] arguments.
 * In addition, variables `1`,`2`,`3` ... would be defined for corresponding groups.
 * It's recommended for all patterns in the same group to have the same set of variables and number of groups, otherwise match result would be hard to use.
 *
 * For example
 * ```
 * {
 *    "regexes": {
 *       "fromName": {
 *          "from": "{team.fullName}",
 *          "rules": {
 *             "(.*)U: ([^(]*)\(.*\)": {
 *                "university": "$1 University",
 *                "teamName": "$2",
 *                "teamParticipants": "$3",
 *             }
 *          }
 *       }
 *       "fromId": {
 *          "from": "{team.id}",
 *          "rules": {
 *             "01(.*)": {"site": "site1", "place":"$1"}
 *             "02(.*)": {"site": "site2", "place":"$1"}
 *             "03(.*)": {"site": "site3", "place":"$1"}
 *          }
 *       }
 *    }
 * }
 * ```
 *
 * For team with id `03456` and name "SomeU: Some Team(SomeName SomeSurname, OtherName OtherSurname)"
 * would define the following variables for other substitutions:
 * * `regexes.fromName.1` - `Some`
 * * `regexes.fromName.university` - `Some University`
 * * `regexes.fromName.2` and `regex.fromName.teamName` - `Some Team`
 * * `regexes.fromName.3` and `regex.fromName.teamParticipants` - `SomeName SomeSurname, OtherName OtherSurname`
 * * `regexes.fromId.site` - `site3`
 * * `regexes.fromId.1` and `regexes.fromId.place` - `456`.
 *
 */
@Serializable
@SerialName("overrideTeamTemplate")
public data class OverrideTeamTemplate(
    public val regexes: Map<String, RegexParser> = emptyMap(),
    public val fullName: String? = null,
    public val groups: List<String>? = null,
    public val extraGroups: List<String>? = null,
    public val organizationId: String? = null,
    public val displayName: String? = null,
    public val hashTag: String? = null,
    public val customFields: Map<String, String>? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val color: String? = null,
): Desugarable, TuningRule {
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return desugar(info).process(info, submittedTeams)
    }

    @OptIn(InefficientContestInfoApi::class)
    override fun desugar(info: ContestInfo): TuningRule {
        return OverrideTeams(
            info.teamList.associate { teamInfo ->
                val extraCustomValues = buildMap {
                    for ((blockName, parser) in regexes) {
                        val fromValue = info.getTemplateValue(parser.from, teamInfo.id, this, false)
                        for ((regex, extras) in parser.rules) {
                            val match = regex.matchEntire(fromValue) ?: continue
                            for ((index, group) in match.groupValues.withIndex().drop(1)) {
                                put("$blockName.$index", group)
                            }
                            for ((key, value) in extras) {
                                put("$blockName.$key", regex.replace(fromValue, value))
                            }
                            break
                        }
                    }
                }
                fun String.ifMatched() = takeUnless { it.contains(templateRegex) }
                fun String.substituted(isUrl: Boolean) = info.getTemplateValue(this, teamInfo.id, extraCustomValues, isUrl)
                teamInfo.id to OverrideTeams.Override(
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

    /**
     * @see [OverrideTeamTemplate]
     */
    @Serializable
    public class RegexParser(
        public val from: String,
        public val rules: Map<Regex, Map<String, String>>,
    )
}