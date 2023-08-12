package org.icpclive.api.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonNames
import org.icpclive.api.*
import org.icpclive.util.*
import java.awt.Color
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
data class TeamInfoOverride(
    @JsonNames("name") val fullName: String? = null,
    @JsonNames("shortname") val displayName: String? = null,
    val groups: List<String>? = null,
    val organizationId: String? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
    val customFields: Map<String, String>? = null,
    val isHidden: Boolean? = null,
    val isOutOfContest: Boolean? = null,
)

/**
 * @param name Problem name.
 * @param color Color of a problem balloon. It would be shown in queue and scoreboard in places related to the problem
 * @param ordinal Number to sort problems in the scoreboard
 * @param minScore In ioi mode minimal possible value of points in this problem
 * @param maxScore In ioi mode maximal possible value of points in this problem
 * @param scoreMergeMode In ioi mode, select the ruleset to calculate final score based on the scores for each submission.
 */
@Serializable
data class ProblemInfoOverride(
    val name: String? = null,
    @Serializable(ColorSerializer::class) val color: Color? = null,
    val ordinal: Int? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val scoreMergeMode: ScoreMergeMode? = null
)

/**
 * @param isHidden Totally hide all teams from this group
 * @param isOutOfContest Teams from this group will be visible everywhere, but will not have any rank assigned to them in leaderboard
 */
@Serializable
data class GroupInfoOverride(
    val isHidden: Boolean? = null,
    val isOutOfContest: Boolean? = null
)

/**
 * @param medals List of awarded medals. They would be allocated in given order, according to rules specified in [MedalType]
 * @param penaltyPerWrongAttempt How many penalty minutes should be added to a team for a wrong attempt
 * @param showTeamsWithoutSubmissions If true, teams without submissions would be automatically hidden
 * @param penaltyRoundingMode Specify rules of how total penalty is calculated based on many submissions
 */
@Serializable
class RankingSettings(
    val medals: List<MedalType>? = null,
    @Serializable(with = DurationInMinutesSerializer::class)
    val penaltyPerWrongAttempt: Duration? = null,
    val showTeamsWithoutSubmissions: Boolean? = null,
    val penaltyRoundingMode: PenaltyRoundingMode? = null
)

@Serializable
data class OrganizationInfoOverride(
    val displayName: String? = null,
    val fullName: String? = null
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
 * @param startTime Override for contest start time.
 *        The preferred format is `yyyy-mm-dd hh:mm:ss`, but some others would be accepted too.
 *        Consider using 'now' if you want the contest emulation to start together with the overlay.
 *        startTime overrise also can affect contest state.
 * @param freezeTime Time from the start of the contest before scoreboard freezing.
 * @param holdTime Fixed time to show as time before the contest start
 * @param teamMediaTemplate Template medias for all teams.
 *        You can use `{teamId}` within the template, it will be substituted with team id from contest system.
 * @param teamRegexes Bunch of regexes to extract information cds doesn't provide from team name.
 * @param teamOverrides Overrides for a specific team. Team id from the contest system is key.
 * @param groupOverrides Overrides for specific groups. Group name is key.
 * @param problemOverrides Overrides for specific problems. Problem letter is key.
 * @param scoreboardOverrides Overrides of scoreboard calculation settings
 */
@Serializable
data class AdvancedProperties(
    @Serializable(with = HumanTimeSerializer::class)
    val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    val holdTime: Duration? = null,
    @Deprecated(level = DeprecationLevel.WARNING, message = "Use teamOverrideTemplate instead")
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrideTemplate: TeamOverrideTemplate? = null,
    val teamRegexes: TeamRegexOverrides? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val groupOverrides: Map<String, GroupInfoOverride>? = null,
    val organizationOverrides: Map<String, OrganizationInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
)

typealias Regex = @Serializable(with = RegexSerializer::class) kotlin.text.Regex

/**
 * All regexes are java regex.
 *
 * Regexes are matched against team full name from cds.
 *
 * @property organizationRegex The only matched group would be equal to new organization id for the team.
 * @property customFields The only group would be set as custom field value for the corresponding key
 * @property groupRegex The group is added if the name matches regex.
 */
@Serializable
data class TeamRegexOverrides(
    val organizationRegex: Regex? = null,
    val customFields: Map<String, Regex>? = null,
    val groupRegex: Map<String, Regex>? = null,
)

@Serializable
data class TeamOverrideTemplate(
    val displayName: String? = null,
    val fullName: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
)

/**
 * Converts values in [ContestInfo] to overrides in [AdvancedProperties
 *
 * @param fields set of fields to include in returned value. Other would be set to null
 */
fun ContestInfo.toAdvancedProperties(fields: Set<String>) : AdvancedProperties {
    fun <T> T.takeIfAsked(name: String) = takeIf { name in fields || "all" in fields }
    return AdvancedProperties(
        startTime = startTime.takeIfAsked("startTime"),
        freezeTime = freezeTime.takeIfAsked("freezeTime"),
        holdTime = holdBeforeStartTime?.takeIfAsked("holdBeforeStartTime"),
        teamOverrides = teams.associate {
            it.contestSystemId to TeamInfoOverride(
                fullName = it.fullName.takeIfAsked("fullName"),
                displayName = it.displayName.takeIfAsked("displayName"),
                groups = it.groups.takeIfAsked("groups"),
                organizationId = it.organizationId.takeIfAsked("organizationId"),
                hashTag = it.hashTag.takeIfAsked("hashTag"),
                medias = it.medias.takeIfAsked("medias"),
                customFields = it.customFields.takeIfAsked("customFields"),
                isHidden = it.isHidden.takeIfAsked("isHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
            )
        },
        problemOverrides = problems.associate {
            it.letter to ProblemInfoOverride(
                name = it.name.takeIfAsked("problemName"),
                color = it.color.takeIfAsked("color"),
                ordinal = it.ordinal.takeIfAsked("ordinal"),
                minScore = it.minScore.takeIfAsked("minScore"),
                maxScore = it.maxScore.takeIfAsked("maxScore"),
                scoreMergeMode = it.scoreMergeMode.takeIfAsked("scoreMergeMode")
            )
        },
        groupOverrides = groups.associate {
            it.name to GroupInfoOverride(
                isHidden = it.isHidden.takeIfAsked("isHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
            )
        },
        organizationOverrides = organizations.associate {
            it.cdsId to OrganizationInfoOverride(
                displayName = it.displayName.takeIfAsked("orgDisplayName"),
                fullName = it.fullName.takeIfAsked("orgFullName")
            )
        },
        scoreboardOverrides = RankingSettings(
            medals = medals.takeIfAsked("medals"),
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        )
    )
}
