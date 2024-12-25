package org.icpclive.cds.tunning

import kotlinx.serialization.Serializable
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

public fun AdvancedProperties.toRulesList(): List<TuningRule> = buildList buildRulesList@{
    if (startTime != null || contestLength != null || freezeTime != null || holdTime != null) {
        add(OverrideTimes(startTime, contestLength, freezeTime, holdTime))
    }
    if (scoreboardOverrides != null && (scoreboardOverrides.penaltyPerWrongAttempt != null || scoreboardOverrides.showTeamsWithoutSubmissions != null || scoreboardOverrides.penaltyRoundingMode != null)) {
        add(OverrideScoreboardSettings(scoreboardOverrides.penaltyPerWrongAttempt, scoreboardOverrides.showTeamsWithoutSubmissions, scoreboardOverrides.penaltyRoundingMode))
    }
    fun TeamRegexOverrides.process(from: String) {
        if (organizationRegex != null) {
            add(SetOrganizationByRegex(from, organizationRegex))
        }
        if (groupRegex != null) {
            for ((group, regex) in groupRegex) {
                add(AddGroupIfMatches(group.toGroupId(), from, regex))
            }
        }
        if (customFields != null) {
            for ((name, regexSet) in customFields) {
                add(AddCustomValueByRegex(name, from, regexSet))
            }
        }
    }
    teamNameRegexes?.process("{team.fullName}")
    teamIdRegexes?.process("{team.id}")
    if (groupOverrides != null && groupOverrides.any { it.value.isHidden != null || it.value.isOutOfContest != null || it.value.displayName != null }) {
        add(OverrideGroups(groupOverrides))
    }
    if (organizationOverrides != null && organizationOverrides.any { it.value.fullName != null || it.value.logo != null || it.value.displayName != null }) {
        if (organizationOverrides.values.all { it.fullName == null && it.logo == null }) {
            add(OverrideOrganizationDisplayNames(organizationOverrides.filterValues { it.displayName != null }.mapValues { it.value.displayName!! }))
        } else {
            add(OverrideOrganizations(organizationOverrides))
        }
    }
    teamOverrides
        ?.values
        ?.flatMap { it.customFields?.keys ?: emptyList() }
        ?.distinct()
        ?.forEach { name ->
            val map = teamOverrides.mapNotNull { (k, v) ->
                v.customFields?.get(name)?.let { k to it }
            }
            add(AddCustomValue(name, map.toMap()))
        }

    if (teamOverrideTemplate != null) {
        add(OverrideTeamTemplate(teamOverrideTemplate.displayName, teamOverrideTemplate.fullName, teamOverrideTemplate.hashTag, teamOverrideTemplate.medias, teamOverrideTemplate.color))
    }

    fun TeamInfoOverride.noComplex(): Boolean {
        if (fullName != null) return false
        if (groups != null) return false
        if (organizationId != null) return false
        if (hashTag != null) return false
        if (medias != null) return false
        if (isHidden != null) return false
        if (isOutOfContest != null) return false
        if (color != null) return false
        return true

    }

    fun TeamInfoOverride.isOnlyCustomFields() : Boolean {
        return noComplex() && displayName == null
    }

    fun TeamInfoOverride.isEmpty() : Boolean {
        return isOnlyCustomFields() && customFields == null
    }

    fun ProblemInfoOverride.onlyColor() : Boolean {
        if (displayName != null) return false
        if (fullName != null) return false
        if (unsolvedColor != null) return false
        if (ordinal != null) return false
        if (minScore != null) return false
        if (maxScore != null) return false
        if (scoreMergeMode != null) return false
        if (isHidden != null) return false
        return true
    }

    val allDisplayNames = teamOverrides?.values?.all { it.displayName != null && it.fullName == null} == true

    if (allDisplayNames) {
        add(OverrideTeamDisplayNames(teamOverrides.mapValues { it.value.displayName!! }))
    }

    teamOverrides
        ?.map { (k, from) ->
            if (from.isOnlyCustomFields()) {
                null
            } else {
                k to TeamInfoOverride(
                    fullName = from.fullName,
                    displayName = from.displayName.takeUnless { allDisplayNames },
                    groups = from.groups,
                    organizationId = from.organizationId,
                    hashTag = from.hashTag,
                    medias = from.medias,
                    customFields = null,
                    isHidden = from.isHidden,
                    isOutOfContest = from.isOutOfContest,
                    color = from.color
                )
            }
        }
        ?.filterNotNull()
        ?.takeIf { it.isNotEmpty() }
        ?.let { m ->
            val mm = m.filter { !it.second.isEmpty() }
            if (mm.isNotEmpty()) {
                add(OverrideTeams(mm.toMap()))
            }
        }

    if (problemOverrides != null && problemOverrides.any { !it.value.onlyColor() || it.value.color != null }) {
        if (problemOverrides.all { it.value.onlyColor() }) {
            add(OverrideProblemColors(problemOverrides.filterValues { it.color != null }.mapValues { (_, v) -> v.color!! }))
        } else {
            add(OverrideProblems(problemOverrides))
        }
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
        for (m in manual) {
            add(AddManualAward(
                m.id,
                m.citation,
                m.teamCdsIds
            ))
        }
        if (championTitle != null || groupChampionTitles.isNotEmpty() || rankAwardsMaxRank != 0 || medals.isNotEmpty()) {
            add(OverrideAwards(
                championTitle = championTitle,
                groupsChampionTitles = groupChampionTitles.takeIf { it.isNotEmpty() },
                rankAwardsMaxRank = rankAwardsMaxRank.takeIf { it != 0 },
                medalGroups = medals.takeIf { it.isNotEmpty() },
            ))
        }
    }
    if (queueSettings != null) {
        add(OverrideQueue(queueSettings.waitTime, queueSettings.firstToSolveWaitTime, queueSettings.featuredRunWaitTime, queueSettings.inProgressRunWaitTime, queueSettings.maxQueueSize, queueSettings.maxUntestedRun))
    }
}