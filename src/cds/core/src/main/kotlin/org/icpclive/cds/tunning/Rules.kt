package org.icpclive.cds.tunning

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.MedalGroup
import java.io.InputStream

@Serializable
public sealed interface TuningRule {
    public companion object {
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }

        public fun listFromString(input: String): List<TuningRule> = json.decodeFromString(input)
        public fun listFromInputStream(input: InputStream): List<TuningRule> = json.decodeFromStream(input)
    }
    public fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo
}

// for schema generation
@Suppress("unused")
@JvmInline
@Serializable
internal value class TuningRuleList(val list: List<TuningRule>)

public sealed interface DesugarableTuningRule : TuningRule {
    public fun desugar(info: ContestInfo): TuningRule
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return desugar(info).process(info, submittedTeams)
    }
}

public sealed interface SimpleDesugarableTuningRule : DesugarableTuningRule {
    public fun desugar(): TuningRule
    override fun desugar(info: ContestInfo): TuningRule = desugar()
}

private fun <K, V> mapOfNotNull(vararg pairs: Pair<K, V?>): Map<K, V> =
    pairs.filter { it.second != null }.associate { it.first to it.second!! }

public fun AdvancedProperties.toRulesList(): List<TuningRule> = buildList buildRulesList@{
    if (contestName != null || startTime != null || contestLength != null || freezeTime != null || holdTime != null) {
        add(OverrideTimes(
            name = contestName,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            holdTime = holdTime
        ))
    }
    if (scoreboardOverrides != null && (scoreboardOverrides.penaltyPerWrongAttempt != null || scoreboardOverrides.showTeamsWithoutSubmissions != null || scoreboardOverrides.penaltyRoundingMode != null)) {
        add(OverrideScoreboardSettings(scoreboardOverrides.penaltyPerWrongAttempt, scoreboardOverrides.showTeamsWithoutSubmissions, scoreboardOverrides.penaltyRoundingMode))
    }
    fun TeamRegexOverrides.process(from: String) {
        if (organizationRegex != null || groupRegex != null || customFields != null) {
            add(
                OverrideTeamTemplate(
                    regexes = mapOfNotNull(
                        "org" to organizationRegex?.regexes?.let {
                            TemplateRegexParser(
                                from = from,
                                rules = it.mapValues { mapOf("id" to it.value) }
                            )
                        },
                        "group" to groupRegex?.entries?.let {
                            TemplateRegexParser(
                                from = from,
                                rules = it.associate { it.value to mapOf("id" to it.key) }
                            )
                        },
                        "custom" to customFields?.let {
                            val data = buildList {
                                for ((k, v) in it) {
                                    for ((regex, replace) in v.regexes) {
                                        add(Triple("${k}Value", regex, replace))
                                    }
                                }
                            }.groupBy({ it.second.toString() }, { it.first to it.third })
                                .mapValues { it.value.toMap() }
                            TemplateRegexParser(
                                from = from,
                                rules = data.mapKeys { Regex(it.key) }
                            )
                        }
                    ),
                    organizationId = "{regexes.org.id}".takeIf { organizationRegex != null },
                    extraGroups = listOf("{regexes.group.id}").takeIf { groupRegex != null },
                    customFields = customFields?.entries?.associate { it.key to "{regexes.custom.${it.key}Value}" }
                )
            )
        }
    }
    teamNameRegexes?.process("{team.fullName}")
    teamIdRegexes?.process("{team.id}")
    if (groupOverrides != null && groupOverrides.any { it.value.isHidden != null || it.value.isOutOfContest != null || it.value.displayName != null }) {
        add(OverrideGroups(groupOverrides))
    }
    if (organizationOverrides != null && organizationOverrides.any { it.value.fullName != null || it.value.logo != null || it.value.displayName != null }) {
        add(OverrideOrganizations(organizationOverrides))
    }

    if (teamOverrideTemplate != null) {
        val template = OverrideTeamTemplate(
            displayName = teamOverrideTemplate.displayName,
            fullName = teamOverrideTemplate.fullName,
            hashTag = teamOverrideTemplate.hashTag,
            medias = teamOverrideTemplate.medias,
            color = teamOverrideTemplate.color
        )
        val serialized = Json.encodeToString(template)
        val templateFixed = Json.decodeFromString<OverrideTeamTemplate>(
            serialized
                .replace("{teamId}", "{team.id}")
                .replace("{orgDisplayName}", "{org.displayName}")
                .replace("{orgFullName}", "{org.fullName}")
        )
        add(templateFixed)
    }

    if (teamOverrides != null) {
        add(OverrideTeams(teamOverrides))
    }

    if (problemOverrides != null) {
        add(OverrideProblems(problemOverrides))
    }
    if (awardsSettings != null) {
        val (championTitle, groupChampionTitles, rankAwardsMaxRank, medalsExtra, medalGroups, manual) = awardsSettings
        fun MedalGroup.tryToMedals() : TuningRule? {
            val goldCut = medals.getOrNull(0)?.maxRank ?: return null
            val silverCut = medals.getOrNull(1)?.maxRank ?: goldCut
            val bronzeCut = medals.getOrNull(2)?.maxRank ?: silverCut
            val cand = AddMedals(goldCut, silverCut - goldCut, bronzeCut - silverCut)
            val des = cand.desugar() as? OverrideAwards ?: return null
            val group = des.extraMedalGroups?.singleOrNull() ?: return null
            return cand.takeIf { group == this }
        }
        val medals = buildList {
            val original = medalGroups + medalsExtra.takeIf { it.isNotEmpty() }?.let { MedalGroup(medalsExtra, emptyList(), emptyList()) }
            for (cand in original.filterNotNull()) {
                val simple: TuningRule? = cand.tryToMedals()
                if (simple != null) {
                    this@buildRulesList.add(simple)
                } else {
                    add(cand)
                }
            }
        }
        if (championTitle != null || groupChampionTitles.isNotEmpty() || rankAwardsMaxRank != 0 || medals.isNotEmpty() || manual.isNotEmpty()) {
            add(OverrideAwards(
                championTitle = championTitle,
                groupsChampionTitles = groupChampionTitles.takeIf { it.isNotEmpty() },
                rankAwardsMaxRank = rankAwardsMaxRank.takeIf { it != 0 },
                medalGroups = medals.takeIf { it.isNotEmpty() },
                manualAwards = manual.takeIf { it.isNotEmpty() },
            ))
        }
    }
    if (queueSettings != null) {
        add(OverrideQueue(queueSettings.waitTime, queueSettings.firstToSolveWaitTime, queueSettings.featuredRunWaitTime, queueSettings.inProgressRunWaitTime, queueSettings.maxQueueSize, queueSettings.maxUntestedRun))
    }
}