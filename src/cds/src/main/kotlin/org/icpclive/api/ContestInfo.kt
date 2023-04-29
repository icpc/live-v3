package org.icpclive.api

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.ColorSerializer
import org.icpclive.util.DurationInMillisecondsSerializer
import org.icpclive.util.UnixMillisecondsSerializer
import org.icpclive.util.getLogger
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class MedalTiebreakMode {
    NONE,
    ALL
}

@Serializable
data class MedalType(
    val name: String,
    val count: Int,
    val minScore: Double = Double.MIN_VALUE,
    val tiebreakMode: MedalTiebreakMode = MedalTiebreakMode.ALL
)

@Serializable
enum class ContestResultType {
    ICPC, IOI
}

enum class ScoreMergeMode {
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
data class ProblemInfo(
    val letter: String,
    val name: String,
    val id: Int,
    val ordinal: Int,
    val contestSystemId: String,
    val minScore: Double? = null,
    val maxScore: Double? = null,
    @Serializable(ColorSerializer::class) val color: Color? = null,
    val scoreMergeMode: ScoreMergeMode? = null,
) {
    companion object {
        val logger = getLogger(ProblemInfo::class)
    }
}

@Serializable
enum class ContestStatus {
    BEFORE, RUNNING, OVER;

    companion object {
        fun byCurrentTime(startTime: Instant, contestLength: Duration, now: Instant = Clock.System.now()): ContestStatus {
            val offset = now - startTime
            return when {
                offset < Duration.ZERO -> BEFORE
                offset < contestLength -> RUNNING
                else -> OVER
            }
        }
    }
}


@Serializable
sealed class MediaType {
    abstract val isMedia: Boolean

    @Serializable
    @SerialName("Photo")
    data class Photo(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Object")
    data class Object(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Video")
    data class Video(val url: String, override val isMedia: Boolean = true) : MediaType()

    /**
     * WebRTC proxy connection
     * @see <a href="https://github.com/kbats183/webrtc-proxy">https://github.com/kbats183/webrtc-proxy</a>
     */
    @Serializable
    @SerialName("WebRTCProxyConnection")
    data class WebRTCProxyConnection(val url: String, val audioUrl: String? = null, override val isMedia: Boolean = true) : MediaType()

    /**
     * WebRTC grabber connection
     * https://github.com/irdkwmnsb/webrtc-grabber
     */
    @Serializable
    @SerialName("WebRTCGrabberConnection")
    data class WebRTCGrabberConnection(
        val url: String,
        val peerName: String,
        val streamType: String,
        val credential: String?,
        override val isMedia: Boolean = true
    ) :
        MediaType()

    @Serializable
    @SerialName("TaskStatus")
    data class TaskStatus(val teamId: Int) : MediaType() {
        override val isMedia = false
    }

    fun applyTemplate(teamId: String) = when (this) {
        is Photo -> copy(url = url.replace("{teamId}", teamId))
        is Video -> copy(url = url.replace("{teamId}", teamId))
        is Object -> copy(url = url.replace("{teamId}", teamId))
        is WebRTCProxyConnection -> copy(url = url.replace("{teamId}", teamId))
        is WebRTCGrabberConnection -> copy(
            url = url.replace("{teamId}", teamId),
            peerName = peerName.replace("{teamId}", teamId),
            credential = credential?.replace("{teamId}", teamId)
        )

        else -> this
    }

    fun noMedia(): MediaType = when (this) {
        is Photo -> copy(isMedia = false)
        is Video -> copy(isMedia = false)
        is Object -> copy(isMedia = false)
        is WebRTCProxyConnection -> copy(isMedia = false)
        is WebRTCGrabberConnection -> copy(isMedia = false)
        else -> this
    }
}

@Serializable
enum class TeamMediaType {
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
data class TeamInfo(
    val id: Int,
    val name: String,
    val shortName: String,
    val contestSystemId: String,
    val groups: List<String>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val additionalInfo: String? = null,
    val isHidden: Boolean = false,
    val isOutOfContest: Boolean = false,
)

@Serializable
data class GroupInfo(
    val name: String,
    val isHidden: Boolean = false,
    val isOutOfContest: Boolean = false
)

@Serializable
enum class PenaltyRoundingMode {
    @SerialName("each_submission_down_to_minute")
    /**
     * Round time of all submissions from the beginning of the contest down to whole minute, and then sum them
     */
    EACH_SUBMISSION_DOWN_TO_MINUTE,

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


@Serializable
data class ContestInfo(
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
    val problems: List<ProblemInfo>,
    val teams: List<TeamInfo>,
    val groups: List<GroupInfo>,
    val penaltyRoundingMode: PenaltyRoundingMode,
    @SerialName("holdBeforeStartTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val holdBeforeStartTime: Duration? = null,
    val emulationSpeed: Double = 1.0,
    val medals: List<MedalType> = emptyList(),
    val penaltyPerWrongAttempt: Duration = 20.minutes,
) {
    val currentContestTime
        get() = when (status) {
            ContestStatus.BEFORE -> Duration.ZERO
            ContestStatus.RUNNING -> (Clock.System.now() - startTime) * emulationSpeed
            ContestStatus.OVER -> contestLength
        }
    fun groupById(id: String) = groups.find { it.name == id }
}

fun List<TeamInfo>.toGroupInfos() = flatMap { it.groups }.distinct().map { GroupInfo(it) }