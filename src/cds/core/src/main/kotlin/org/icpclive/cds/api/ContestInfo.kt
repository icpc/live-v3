package org.icpclive.cds.api

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.util.*
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Serializable
public enum class ContestResultType {
    ICPC, IOI
}

public enum class ScoreMergeMode {
    /**
     * For each tests group in the problem, get maximum score over all submissions.
     */
    MAX_PER_GROUP,

    /**
     * Get maximum total score over all submissions
     */
    MAX_TOTAL,

    /**
     * Get score from last submission
     */
    LAST,

    /**
     * Get score from last submissions, ignoring submissions, which didn't pass preliminary testing (e.g. on sample tests)
     */
    LAST_OK,

    /**
     * Get the sum of scores over all submissions
     */
    SUM
}

@Serializable
public data class ProblemInfo(
    val id: String,
    @SerialName("letter") val displayName: String,
    @SerialName("name") val fullName: String,
    val ordinal: Int,
    @Required val minScore: Double? = null,
    @Required val maxScore: Double? = null,
    @Required @Serializable(ColorSerializer::class) val color: Color? = null,
    @Required @Serializable(ColorSerializer::class) val unsolvedColor: Color? = null,
    @Required val scoreMergeMode: ScoreMergeMode? = null,
    @Required val isHidden: Boolean = false,
)

@Serializable
public enum class ContestStatus {
    BEFORE, RUNNING, FAKE_RUNNING, OVER, FINALIZED;

    public companion object {
        public fun byCurrentTime(startTime: Instant, contestLength: Duration): ContestStatus {
            val offset = Clock.System.now() - startTime
            return when {
                offset < Duration.ZERO -> BEFORE
                offset < contestLength -> RUNNING
                else -> OVER
            }
        }
    }
}


@Serializable
public sealed class MediaType {
    public abstract val isMedia: Boolean

    @Serializable
    @SerialName("Photo")
    public data class Photo(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Object")
    public data class Object(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Video")
    public data class Video(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("M2tsVideo")
    public data class M2tsVideo(val url: String, override val isMedia: Boolean = true) : MediaType()

    /**
     * WebRTC proxy connection
     * @see <a href="https://github.com/kbats183/webrtc-proxy">https://github.com/kbats183/webrtc-proxy</a>
     */
    @Serializable
    @SerialName("WebRTCProxyConnection")
    public data class WebRTCProxyConnection(
        val url: String,
        val audioUrl: String? = null,
        override val isMedia: Boolean = true,
    ) : MediaType()

    /**
     * WebRTC grabber connection
     * https://github.com/irdkwmnsb/webrtc-grabber
     */
    @Serializable
    @SerialName("WebRTCGrabberConnection")
    public data class WebRTCGrabberConnection(
        val url: String,
        val peerName: String,
        val streamType: String,
        val credential: String?,
        override val isMedia: Boolean = true,
    ) :
        MediaType()

    @Serializable
    @SerialName("TaskStatus")
    public data class TaskStatus(val teamId: Int) : MediaType() {
        override val isMedia: Boolean = false
    }

    public fun noMedia(): MediaType = when (this) {
        is Photo -> copy(isMedia = false)
        is Video -> copy(isMedia = false)
        is Object -> copy(isMedia = false)
        is M2tsVideo -> copy(isMedia = false)
        is WebRTCProxyConnection -> copy(isMedia = false)
        is WebRTCGrabberConnection -> copy(isMedia = false)
        else -> this
    }
}

@Serializable
public enum class TeamMediaType {
    @SerialName("camera")
    CAMERA,

    @SerialName("screen")
    SCREEN,

    @SerialName("record")
    RECORD,

    @SerialName("photo")
    PHOTO,

    @SerialName("reactionVideo")
    REACTION_VIDEO,

    @SerialName("achievement")
    ACHIEVEMENT,
}

@Serializable
public data class TeamInfo(
    val id: Int,
    @SerialName("name") val fullName: String,
    @SerialName("shortName") val displayName: String,
    val contestSystemId: String,
    val groups: List<String>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
    val organizationId: String?,
    @Required val customFields: Map<String, String> = emptyMap(),
)

@Serializable
public data class GroupInfo(
    val cdsId: String,
    val displayName: String,
    val isHidden: Boolean,
    val isOutOfContest: Boolean,
)

@Serializable
public data class OrganizationInfo(
    val cdsId: String,
    val displayName: String,
    val fullName: String,
    val logo: MediaType?,
)

@Serializable
public enum class PenaltyRoundingMode {
    /**
     * Round time of all submissions from the beginning of the contest down to whole minute, and then sum them
     */
    @SerialName("each_submission_down_to_minute")
    EACH_SUBMISSION_DOWN_TO_MINUTE,

    /**
     * Round time of all submissions from the beginning of the contest up to whole minute, and then sum them
     */
    @SerialName("each_submission_up_to_minute")
    EACH_SUBMISSION_UP_TO_MINUTE,

    /**
     * Sum time of all submissions from the beginning of the contest and then round it down to whole minute
     */
    @SerialName("sum_down_to_minute")
    SUM_DOWN_TO_MINUTE,

    /**
     * Sum time of all submissions without rounding
     */
    @SerialName("sum_in_seconds")
    SUM_IN_SECONDS,

    /**
     * Get time of last submission as penalty
     */
    @SerialName("last")
    LAST,

    /**
     * Don't have any penalty as a tie-breaker
     */
    @SerialName("zero")
    ZERO,
}

/**
 * @param championTitle If not null, the winner award with the corresponding title would be generated
 * @param groupsChampionTitles For group ids used as keys, a group champion award with corresponding value as title would be generated
 * @param rankAwardsMaxRank For first [rankAwardsMaxRank] places award stating "this place" would be awarded
 * @param medals An extra element in the [medalGroups] list for more convenient config in case of a single group.
 * @param medalGroups List of rank and score-based awards. Only the first applicable in each list is awarded to a team.
 * @param manual List of awards with a manual team list.
 */
@Serializable
public data class AwardsSettings(
    public val championTitle: String? = null,
    public val groupsChampionTitles: Map<String, String> = emptyMap(),
    public val rankAwardsMaxRank: Int = 0,
    private val medals: List<MedalSettings> = emptyList(),
    private val medalGroups: List<List<MedalSettings>> = emptyList(),
    public val manual: List<ManualAwardSetting> = emptyList(),
) {
    @Transient
    public val medalSettings: List<List<MedalSettings>> = buildList {
        addAll(medalGroups)
        if (medals.isNotEmpty()) {
            add(medals)
        }
    }

    public enum class MedalTiebreakMode { NONE, ALL; }

    /**
     * Settings for rank and score-based awards. Typically, medals, diplomas and honorable mentions.
     * For the purpose of this API, all of them are called medals.
     *
     * @param id ID of award
     * @param citation Award text.
     * @param color Color of award to hightlight teams in overlay
     * @param maxRank If not null, only teams with rank of at most [maxRank] are eligible.
     * @param minScore Only teams with score of at leat [minScore] are eligible. By default, any non-zero score is required to receive a medal.
     * @param tiebreakMode In case of tied ranks, if [MedalTiebreakMode.NONE] none of the teams will be awarded, if [MedalTiebreakMode.ALL] - all.
     */
    @Serializable
    public data class MedalSettings(
        val id: String,
        val citation: String,
        val color: Award.Medal.MedalColor? = null,
        val maxRank: Int? = null,
        val minScore: Double = Double.MIN_VALUE,
        val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL,
    )

    /**
     * Settings for awards granted manually.
     *
     * @param id ID of award
     * @param citation Award text
     * @param teamCdsIds List of team cds ids to grant award.
     */
    @Serializable
    public data class ManualAwardSetting(
        val id: String,
        val citation: String,
        val teamCdsIds: List<String>,
    )
}

@Target(AnnotationTarget.PROPERTY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This api is not efficient in most cases, consider using corresponding map instead"
)
public annotation class InefficientContestInfoApi

@Serializable
@OptIn(InefficientContestInfoApi::class)
public data class ContestInfo(
    val name: String,
    val status: ContestStatus,
    val resultType: ContestResultType,
    @SerialName("startTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val startTime: Instant,
    @SerialName("contestLengthMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val contestLength: Duration,
    @SerialName("freezeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val freezeTime: Duration,
    @InefficientContestInfoApi @SerialName("problems") val problemList: List<ProblemInfo>,
    @InefficientContestInfoApi @SerialName("teams") val teamList: List<TeamInfo>,
    @InefficientContestInfoApi @SerialName("groups") val groupList: List<GroupInfo>,
    @InefficientContestInfoApi @SerialName("organizations") val organizationList: List<OrganizationInfo>,
    val penaltyRoundingMode: PenaltyRoundingMode,
    @SerialName("holdBeforeStartTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @Required val holdBeforeStartTime: Duration? = null,
    @Required val emulationSpeed: Double = 1.0,
    @Required val awardsSettings: AwardsSettings = AwardsSettings(),
    @Required val penaltyPerWrongAttempt: Duration = 20.minutes,
    @Transient val cdsSupportsFinalization: Boolean = false,
) {
    public val currentContestTime: Duration
        get() = when (status) {
            ContestStatus.BEFORE -> Duration.ZERO
            ContestStatus.RUNNING, ContestStatus.FAKE_RUNNING -> (Clock.System.now() - startTime) * emulationSpeed
            ContestStatus.OVER, ContestStatus.FINALIZED -> contestLength
        }
    val groups: Map<String, GroupInfo> by lazy { groupList.associateBy { it.cdsId } }
    val teams: Map<Int, TeamInfo> by lazy { teamList.associateBy { it.id } }
    val cdsTeams: Map<String, TeamInfo> by lazy { teamList.associateBy { it.contestSystemId } }
    val organizations: Map<String, OrganizationInfo> by lazy { organizationList.associateBy { it.cdsId } }
    val problems: Map<String, ProblemInfo> by lazy { problemList.associateBy { it.id } }
    val scoreboardProblems: List<ProblemInfo> by lazy { problemList.sortedBy { it.ordinal }.filterNot { it.isHidden } }
}