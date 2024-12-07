package org.icpclive.cds.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.*
import java.io.InputStream
import kotlin.time.Duration

@Serializable
public sealed interface TuningRule {
    public companion object {
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }

        public fun fromString(input: String): List<TuningRule> = json.decodeFromString(input)
        public fun fromInputStream(input: InputStream): List<TuningRule> = json.decodeFromStream(input)
    }
}

@Serializable
@SerialName("override_teams")
public data class OverrideTeams(public val rules: Map<TeamId, TeamInfoOverride>): TuningRule
@Serializable
@SerialName("override_problems")
public data class OverrideProblems(public val rules: Map<ProblemId, ProblemInfoOverride>): TuningRule
@Serializable
@SerialName("override_groups")
public data class OverrideGroups(public val rules: Map<GroupId, GroupInfoOverride>): TuningRule
@Serializable
@SerialName("override_organizations")
public data class OverrideOrganizations(public val rules: Map<OrganizationId, OrganizationInfoOverride>): TuningRule

@Serializable
@SerialName("override_times")
public data class OverrideTimes(
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("contestLengthSeconds")
    public val contestLength: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    public val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    public val holdTime: Duration? = null,
): TuningRule

@Serializable
@SerialName("override_queue")
public data class OverrideQueue(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("waitTimeSeconds")
    public val waitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("firstToSolveWaitTimeSeconds")
    public val firstToSolveWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("featuredRunWaitTimeSeconds")
    public val featuredRunWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("inProgressRunWaitTimeSeconds")
    public val inProgressRunWaitTime: Duration? = null,
    public val maxQueueSize: Int? = null,
    public val maxUntestedRun: Int? = null,
): TuningRule

@Serializable
@SerialName("override_awards")
public data class OverrideAwards(public val settings: AwardsSettings): TuningRule

@Serializable
@SerialName("override_team_template")
public data class OverrideTeamTemplate(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val hashTag: String? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val color: String? = null,
): TuningRule

@Serializable
@SerialName("set_organization_by_regex")
public data class SetOrganizationByRegex(public val from: String, public val rules: RegexSet): TuningRule

@Serializable
@SerialName("add_group_if_matches")
public data class AddGroupIfMatches(public val from: String, public val rule: Regex, public val id: GroupId): TuningRule

@Serializable
@SerialName("add_custom_value_by_regex")
public data class AddCustomValueByRegex(
    public val name: String,
    public val from: String,
    public val rules: RegexSet
): TuningRule

@Serializable
@SerialName("add_custom_value")
public data class AddCustomValue(
    public val name: String,
    public val rules: Map<TeamId, String>
): TuningRule

@Serializable
@SerialName("override_scoreboard_settings")
public data class OverrideScoreboardSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    public val penaltyPerWrongAttempt: Duration? = null,
    public val showTeamsWithoutSubmissions: Boolean? = null,
    public val penaltyRoundingMode: PenaltyRoundingMode? = null,
): TuningRule

@Serializable
@SerialName("override_team_display_names")
public data class OverrideTeamDisplayNames(
    val rules: Map<TeamId, String>
): TuningRule

@Serializable
@SerialName("override_organization_display_names")
public data class OverrideOrganizationDisplayNames(
    val rules: Map<OrganizationId, String>
): TuningRule


@Serializable
@SerialName("override_problem_colors")
public data class OverrideProblemColors(
    val rules: Map<ProblemId, Color>
): TuningRule

public fun AdvancedProperties.toRulesList(): List<TuningRule> = buildList {
    if (startTime != null || contestLength != null || freezeTime != null || holdTime != null) {
        add(OverrideTimes(startTime, contestLength, freezeTime, holdTime))
    }
    if (scoreboardOverrides != null) {
        add(OverrideScoreboardSettings(scoreboardOverrides.penaltyPerWrongAttempt, scoreboardOverrides.showTeamsWithoutSubmissions, scoreboardOverrides.penaltyRoundingMode))
    }
    fun TeamRegexOverrides.process(from: String) {
        if (organizationRegex != null) {
            add(SetOrganizationByRegex(from, organizationRegex))
        }
        if (groupRegex != null) {
            for ((group, regex) in groupRegex) {
                add(AddGroupIfMatches(from, regex, group.toGroupId()))
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
    if (groupOverrides != null) {
        add(OverrideGroups(groupOverrides))
    }
    if (organizationOverrides != null) {
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
        add(OverrideTeamDisplayNames(teamOverrides!!.mapValues { it.value.displayName!! }))
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

    if (problemOverrides != null) {
        if (problemOverrides.all { it.value.onlyColor() }) {
            add(OverrideProblemColors(problemOverrides.filterValues { it.color != null }.mapValues { (k, v) -> v.color!! }))
        } else {
            add(OverrideProblems(problemOverrides))
        }
    }
    if (awardsSettings != null) {
        add(OverrideAwards(awardsSettings))
    }
    if (queueSettings != null) {
        add(OverrideQueue(queueSettings.waitTime, queueSettings.firstToSolveWaitTime, queueSettings.featuredRunWaitTime, queueSettings.inProgressRunWaitTime, queueSettings.maxQueueSize, queueSettings.maxUntestedRun))
    }
}