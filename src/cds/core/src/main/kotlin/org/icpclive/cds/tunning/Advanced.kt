package org.icpclive.cds.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.icpclive.cds.api.*
import org.icpclive.cds.api.AwardsSettings.ManualAwardSetting
import org.icpclive.cds.api.AwardsSettings.MedalGroup
import org.icpclive.cds.api.AwardsSettings.MedalSettings
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.serializers.RegexSerializer
import org.icpclive.cds.util.serializers.*
import java.io.InputStream
import kotlin.time.Duration

@Serializable
public class RankingSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    public val penaltyPerWrongAttempt: Duration? = null,
    public val showTeamsWithoutSubmissions: Boolean? = null,
    public val penaltyRoundingMode: PenaltyRoundingMode? = null,
)


/**
 * This class represents possible contest configuration overrides.
 *
 * Ideally, all this information should be received from the contest system.
 * Unfortunately, in the real world, it's not always possible, or information
 * can be not fully correct or convenient to display.
 *
 * This class contains the data to be fixed in what is received from a contest system.
 *
 * The order in which overrides applied:
 *   * Time and scoreboard related (they don't interact with others)
 *   * Filtering out non-submitted teams if requested
 *   * Regexp overrides by team name (so values provided by them can be used in templates)
 *   * Regexp overrides by team id (so values provided by them can be used in templates)
 *   * Creating new groups mentioned in teams or overrides and group overrides
 *   * Creating new organizations mentioned in teams or overrides and organization overrides
 *   * Custom fields from team overrides (so values provided by them can be used in templates)
 *   * (Deprecated) Team media template
 *   * Team override template
 *   * Normal team overrides
 *
 * In all templates in all strings inside `{variableName}` pattern is substituted.
 * The following variable names are supported:
 *   * teamId - team id from the contest system
 *   * orgFullName - fullName of team organization
 *   * orgDisplayName - displayName of team organization
 *   * all values from customFields
 *
 * @param startTime Override for contest start time.
 *        The preferred format is `yyyy-mm-dd hh:mm:ss`, but some others would be accepted too.
 *        startTime override also can affect contest state.
 * @param contestLength Length of the contest. Also, can affect contest state.
 * @param freezeTime Time from the start of the contest before scoreboard freezing.
 * @param holdTime Fixed time to show as time before the contest start
 * @param teamOverrideTemplate Template for team overrides
 * @param teamNameRegexes Bunch of regexes to extract information cds doesn't provide from team name.
 * @param teamIdRegexes Bunch of regexes to extract information cds doesn't provide from team's ID.
 * @param teamOverrides Overrides for a specific team. Team id from the contest system is key.
 * @param groupOverrides Overrides for specific groups. Group name is key.
 * @param problemOverrides Overrides for specific problems. Problem id from the contest system is key.
 * @param scoreboardOverrides Overrides of scoreboard calculation settings
 */
@Serializable
public class AdvancedProperties(
    public val contestName: String? = null,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    public val contestLength: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    public val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    public val holdTime: Duration? = null,
    public val teamOverrideTemplate: TeamOverrideTemplate? = null,
    public val teamNameRegexes: TeamRegexOverrides? = null,
    public val teamIdRegexes: TeamRegexOverrides? = null,
    public val teamOverrides: Map<TeamId, OverrideTeams.Override>? = null,
    public val groupOverrides: Map<GroupId, OverrideGroups.Override>? = null,
    public val organizationOverrides: Map<OrganizationId, OverrideOrganizations.Override>? = null,
    public val problemOverrides: Map<ProblemId, OverrideProblems.Override>? = null,
    public val scoreboardOverrides: RankingSettings? = null,
    public val awardsSettings: AwardsSettingsOverride? = null,
    public val queueSettings: QueueSettingsOverride? = null,
) {
    public companion object {
        private val json = Json {
            allowComments = true
            allowTrailingComma = true
        }
        public fun fromString(input: String): AdvancedProperties = json.decodeFromString<AdvancedProperties>(input)
        public fun fromInputStream(input: InputStream): AdvancedProperties = json.decodeFromStream<AdvancedProperties>(input)
    }
}

internal typealias Regex = @Serializable(with = RegexSerializer::class) kotlin.text.Regex

@Serializable
@JvmInline
public value class RegexSet(public val regexes: Map<Regex, String>) {
    public fun applyTo(data: String): String? {
        val matched = regexes.entries.filter { data.matches(it.key) }
        return when (matched.size) {
            0 -> {
                logger.warning { "None of regexes ${regexes.map { it.key }} didn't match $this" }
                null
            }

            1 -> {
                val (regex, replace) = matched.single()
                try {
                    data.replace(regex, replace)
                } catch (e: RuntimeException) {
                    logger.warning { "Failed to apply $regex -> $replace to ${this}: ${e.message}" }
                    null
                }
            }

            else -> {
                logger.warning { "Multiple regexes ${matched.map { it.key }} match $this" }
                null
            }
        }
    }

    private companion object {
        val logger by getLogger()
    }
}

/**
 * In some cases, the contest system provides some useful information as part of the team name.
 * This can be used to extract this information to something more structured.
 *
 * All regexes are java regex.
 *
 * @property organizationRegex The only matched regex replacement would be equal to new organization id for the team.
 * @property customFields The only matched regex replacement would be set as custom field value for the corresponding key
 * @property groupRegex The group is added if the name matches regex.
 */
@Serializable
public class TeamRegexOverrides(
    public val organizationRegex: RegexSet? = null,
    public val customFields: Map<String, RegexSet>? = null,
    public val groupRegex: Map<String, Regex>? = null,
)

@Serializable
public class TeamOverrideTemplate(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val hashTag: String? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val color: String? = null,
)

@Serializable
public data class QueueSettingsOverride(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("waitTimeSeconds")
    val waitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("firstToSolveWaitTimeSeconds")
    val firstToSolveWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("featuredRunWaitTimeSeconds")
    val featuredRunWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("inProgressRunWaitTimeSeconds")
    val inProgressRunWaitTime: Duration? = null,
    val maxQueueSize: Int? = null,
    val maxUntestedRun: Int? = null,
)

@Serializable
public data class AwardsSettingsOverride(
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<GroupId, String> = emptyMap(),
    public val rankAwardsMaxRank: Int = 0,
    public val medals: List<MedalSettings> = emptyList(),
    public val medalGroups: List<MedalGroup> = emptyList(),
    public val manual: List<ManualAwardSetting> = emptyList(),
)

/**
 * Converts values in [ContestInfo] to overrides in [AdvancedProperties
 *
 * @param fields set of fields to include in returned value. Other would be set to null
 */
@OptIn(InefficientContestInfoApi::class)
public fun ContestInfo.toAdvancedProperties(fields: Set<String>): AdvancedProperties {
    fun <T> T.takeIfAsked(name: String) = takeIf { name in fields || "all" in fields }
    return AdvancedProperties(
        startTime = startTime.takeIfAsked("startTime"),
        contestLength = contestLength.takeIfAsked("contestLength"),
        freezeTime = freezeTime.takeIfAsked("freezeTime"),
        holdTime = (status as? ContestStatus.BEFORE)?.holdTime?.takeIfAsked("holdBeforeStartTime"),
        teamOverrides = teamList.associate {
            it.id to OverrideTeams.Override(
                fullName = it.fullName.takeIfAsked("fullName"),
                displayName = it.displayName.takeIfAsked("displayName"),
                groups = it.groups.takeIfAsked("groups"),
                organizationId = it.organizationId.takeIfAsked("organizationId"),
                hashTag = it.hashTag.takeIfAsked("hashTag"),
                medias = it.medias.takeIfAsked("medias"),
                customFields = it.customFields.takeIfAsked("customFields"),
                isHidden = it.isHidden.takeIfAsked("teamIsHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
            )
        },
        problemOverrides = problemList.associate {
            it.id to OverrideProblems.Override(
                displayName = it.displayName.takeIfAsked("problemDisplayName"),
                fullName = it.fullName.takeIfAsked("problemFullName"),
                color = it.color.takeIfAsked("color"),
                unsolvedColor = it.color.takeIfAsked("unsolvedColor"),
                ordinal = it.ordinal.takeIfAsked("ordinal"),
                minScore = it.minScore.takeIfAsked("minScore"),
                maxScore = it.maxScore.takeIfAsked("maxScore"),
                scoreMergeMode = it.scoreMergeMode.takeIfAsked("scoreMergeMode"),
                isHidden = it.isHidden.takeIfAsked("problemIsHidden")
            )
        },
        groupOverrides = groupList.associate {
            it.id to OverrideGroups.Override(
                displayName = it.displayName.takeIfAsked("groupDisplayName"),
                isHidden = it.isHidden.takeIfAsked("groupIsHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest"),
            )
        },
        organizationOverrides = organizationList.associate {
            it.id to OverrideOrganizations.Override(
                displayName = it.displayName.takeIfAsked("orgDisplayName"),
                fullName = it.fullName.takeIfAsked("orgFullName"),
                logo = it.logo.takeIfAsked("logo")
            )
        },
        scoreboardOverrides = RankingSettings(
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        ),
        awardsSettings = AwardsSettingsOverride(
            championTitle = awardsSettings.championTitle,
            groupsChampionTitles = awardsSettings.groupsChampionTitles,
            rankAwardsMaxRank = awardsSettings.rankAwardsMaxRank,
            medals = emptyList(),
            medalGroups = awardsSettings.medalGroups,
            manual = awardsSettings.manual,
        ).takeIfAsked("awards"),
        queueSettings = QueueSettingsOverride(
            waitTime = queueSettings.waitTime,
            firstToSolveWaitTime = queueSettings.firstToSolveWaitTime,
            featuredRunWaitTime = queueSettings.featuredRunWaitTime,
            inProgressRunWaitTime = queueSettings.inProgressRunWaitTime,
            maxQueueSize = queueSettings.maxQueueSize,
            maxUntestedRun = queueSettings.maxUntestedRun,
        ).takeIfAsked("queue")
    )
}
