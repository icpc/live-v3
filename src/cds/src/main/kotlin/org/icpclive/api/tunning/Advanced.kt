package org.icpclive.api.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.util.*
import java.awt.Color
import kotlin.time.Duration

/**
 * @param name Full name of the team. Will be mostly shown on admin pages.
 * @param shortname Name of the team shown in most places.
 * @param groups The list of the groups team belongs too.
 * @param hashTag Team hashtag. Can be shown on some team related pages
 * @param medias Map of urls to team related medias. E.g., team photo or some kind of video from workstation.
 *               If media is explicitly set to null, it would be removed if received from a contest system.
 * @param additionalInfo
 * @param isHidden If set to true, team would be totaly hidden.
 * @param isOutOfContest If set to true, team would not recieve rank in scoreboard, but it's submission would still be shown.
 */
@Serializable
data class TeamInfoOverride(
    val name: String? = null,
    val shortname: String? = null,
    val groups: List<String>? = null,
    val hashTag: String? = null,
    val medias: Map<TeamMediaType, MediaType?>? = null,
    val additionalInfo: String? = null,
    val isHidden: Boolean? = null,
    val isOutOfContest: Boolean? = null
)

/**
 * @param name Problem name.
 * @param color Color of a problem balloon. It would be shown in queue and scoreboard in places related to the problem
 * @param minScore In ioi mode minimal possible value of points in this problem
 * @param maxScore In ioi mode maximal possible value of points in this problem
 * @param scoreMergeMode In ioi mode, it determinate how final points for participant are calculated from points of many submissions.
 */
@Serializable
data class ProblemInfoOverride(
    val name: String? = null,
    @Serializable(ColorSerializer::class)
    val color: Color? = null,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    val scoreMergeMode: ScoreMergeMode? = null
)

/**
 * @param isHidden Totally hide all teams from the group
 * @param isOutOfContest Don't get rank in the scoreboard to all teams from the group
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

/**
 * This class represents possible overrides.
 *
 * For most cases, all this information should be received from the contest system.
 * Unfortunately, in the real world, it's not always possible, or information
 * can be not fully correct on convenient to display.
 *
 * This class contains the data to be fixed in what is received from a contest system.
 *
 * @param startTime Override for contest start time.
 *        The preferred format is `yyyy-mm-dd hh:mm:ss`, but some others would be accepted too.
 *        It also affects contest state if overridden.
 * @param freezeTime Time from the start of the contest before scoreboard freezing.
 * @param holdTime Fixed time to show as time before the contest start
 * @param teamMediaTemplate Template medias for all teams.
 *        One can use `{teamId}` inside, it would be replaced with team id from contest system.
 * @param teamOverrides Overrides for specific teams. Team id from the contest system is key.
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
    val teamMediaTemplate: Map<TeamMediaType, MediaType?>? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val groupOverrides: Map<String, GroupInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null,
    val scoreboardOverrides: RankingSettings? = null
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
                name = it.name.takeIfAsked("name"),
                shortname = it.shortName.takeIfAsked("shortname"),
                groups = it.groups.takeIfAsked("groups"),
                hashTag = it.hashTag.takeIfAsked("hashTag"),
                medias = it.medias.takeIfAsked("medias"),
                isHidden = it.isHidden.takeIfAsked("isHidden"),
                isOutOfContest = it.isOutOfContest.takeIfAsked("isOutOfContest")
            )
        },
        problemOverrides = problems.associate {
            it.letter to ProblemInfoOverride(
                name = it.name.takeIfAsked("problemName"),
                color = it.color.takeIfAsked("color"),
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
        scoreboardOverrides = RankingSettings(
            medals = medals.takeIfAsked("medals"),
            penaltyPerWrongAttempt = penaltyPerWrongAttempt.takeIfAsked("penaltyPerWrongAttempt"),
            penaltyRoundingMode = penaltyRoundingMode.takeIfAsked("penaltyRoundingMode")
        )
    )
}