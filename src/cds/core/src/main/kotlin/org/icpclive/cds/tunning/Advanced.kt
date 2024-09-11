package org.icpclive.cds.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.RegexSerializer
import org.icpclive.cds.util.serializers.*
import java.io.InputStream
import kotlin.time.Duration

/**
 * @param fullName Full name of the team. Will be mostly shown on admin pages.
 * @param displayName Name of the team shown in most places.
 * @param groups The list of the groups team belongs too.
 * @param organizationId The id of organization team comes from
 * @param hashTag Team hashtag. Can be shown on some team related pages
 * @param medias Map of urls to team related medias. E.g., team photo or some kind of video from workstation.
 *               If media is explicitly set to null, it would be removed if received from a contest system.
 * @param customFields Map of custom values. They can be used in substitutions in templates.
 * @param isHidden If set to true, the team would be totally hidden.
 * @param isOutOfContest If set to true, the team would not receive rank in scoreboard, but it's submission would still be shown.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
public class TeamInfoOverride(
    @JsonNames("name") public val fullName: String? = null,
    @JsonNames("shortname") public val displayName: String? = null,
    public val groups: List<GroupId>? = null,
    public val organizationId: OrganizationId? = null,
    public val hashTag: String? = null,
    public val medias: Map<TeamMediaType, MediaType?>? = null,
    public val customFields: Map<String, String>? = null,
    public val isHidden: Boolean? = null,
    public val isOutOfContest: Boolean? = null,
    public val color: Color? = null
)

/**
 * @param displayName Name to show in scoreboard and queue.
 * @param fullName Problem name.
 * @param color Color of a problem balloon. It would be shown in queue and scoreboard in places related to the problem
 * @param ordinal Number to sort problems in the scoreboard
 * @param minScore In ioi mode minimal possible value of points in this problem
 * @param maxScore In ioi mode maximal possible value of points in this problem
 * @param scoreMergeMode In ioi mode, select the ruleset to calculate the final score based on the scores for each submission.
 * @param isHidden If true, ignore all runs on that problem and remove it from scoreboard.
 */
@Serializable
public class ProblemInfoOverride(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val color: Color? = null,
    public val unsolvedColor: Color? = null,
    public val ordinal: Int? = null,
    public val minScore: Double? = null,
    public val maxScore: Double? = null,
    public val scoreMergeMode: ScoreMergeMode? = null,
    public val isHidden: Boolean? = null,
)

/**
 * @param displayName Name of the group to be displayed in admin and export
 * @param isHidden Totally hide all teams from this group
 * @param isOutOfContest Teams from this group will be visible everywhere, but will not have any rank assigned to them in the leaderboard
 */
@Serializable
public class GroupInfoOverride(
    public val displayName: String? = null,
    public val isHidden: Boolean? = null,
    public val isOutOfContest: Boolean? = null,
)

/**
 * @param penaltyPerWrongAttempt How many penalty minutes should be added to a team for a wrong attempt
 * @param showTeamsWithoutSubmissions If true, teams without submissions would be automatically hidden
 * @param penaltyRoundingMode Specify rules of how total penalty is calculated based on many submissions
 */
@Serializable
public class RankingSettings(
    @Serializable(with = DurationInMinutesSerializer::class)
    public val penaltyPerWrongAttempt: Duration? = null,
    public val showTeamsWithoutSubmissions: Boolean? = null,
    public val penaltyRoundingMode: PenaltyRoundingMode? = null,
)


/**
 * @param displayName Name of the team shown in most places.
 * @param fullName Full name of the organization. Will be mostly shown on admin pages.
 * @param logo Organization logo. Not displayed anywhere for now, but can be exported to e.g., icpc resolved.
 */
@Serializable
public class OrganizationInfoOverride(
    public val displayName: String? = null,
    public val fullName: String? = null,
    public val logo: MediaType? = null,
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
    public val teamOverrides: Map<TeamId, TeamInfoOverride>? = null,
    public val groupOverrides: Map<GroupId, GroupInfoOverride>? = null,
    public val organizationOverrides: Map<OrganizationId, OrganizationInfoOverride>? = null,
    public val problemOverrides: Map<ProblemId, ProblemInfoOverride>? = null,
    public val scoreboardOverrides: RankingSettings? = null,
    public val awardsSettings: AwardsSettings? = null,
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
public value class RegexSet(public val regexes: Map<Regex, String>)

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

/**
 * Template for the team override.
 *
 * It has smaller priority than override in the team itself.
 *
 * Check [AdvancedProperties] doc about patterns inside template.
 *
 * @property displayName Template string for team display name. Check [TeamInfoOverride.displayName] for details.
 * @property fullName Template string for team full name. Check [TeamInfoOverride.fullName] for details.
 * @property medias Templates for team medias. Check [TeamInfoOverride.medias] for details.
 */
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
            it.id to TeamInfoOverride(
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
            it.id to ProblemInfoOverride(
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
            it.id to GroupInfoOverride(
                displayName = it.displayName.takeIfAsked("groupDisplayName"),
                isHidden = it.isHidden.takeIfAsked("groupIsHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest"),
            )
        },
        organizationOverrides = organizationList.associate {
            it.id to OrganizationInfoOverride(
                displayName = it.displayName.takeIfAsked("orgDisplayName"),
                fullName = it.fullName.takeIfAsked("orgFullName"),
                logo = it.logo.takeIfAsked("logo")
            )
        },
        scoreboardOverrides = RankingSettings(
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        ),
        awardsSettings = awardsSettings.takeIfAsked("awards"),
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
