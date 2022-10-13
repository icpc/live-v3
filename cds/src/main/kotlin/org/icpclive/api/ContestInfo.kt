package org.icpclive.api

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.common.util.ColorSerializer
import org.icpclive.common.util.DurationInMillisecondsSerializer
import org.icpclive.common.util.UnixMillisecondsSerializer
import org.icpclive.common.util.getLogger
import java.awt.Color
import java.lang.NumberFormatException
import kotlin.time.Duration

@Serializable
data class MedalType(val name: String, val count: Int)

@Serializable
data class ProblemInfo(
    val letter: String,
    val name: String,
    @Serializable(ColorSerializer::class) val color: Color,
    val id: Int,
    val ordinal: Int
) {
    constructor(letter: String, name: String, color: String?, id: Int, ordinal: Int) :
            this(letter, name, parseColor(color) ?: Color.BLACK, id, ordinal)


    companion object {
        fun parseColor(color: String?): Color? = try {
            when {
                color == null -> null
                color.startsWith("0x") -> Color.decode(color)
                color.startsWith("#") -> Color.decode("0x" + color.substring(1))
                else -> Color.decode("0x$color")
            }
        } catch (e: NumberFormatException) {
            logger.warn("Failed to parse color $color")
            null
        }

        val logger = getLogger(ProblemInfo::class)
    }
}

@Serializable
enum class ContestStatus {
    UNKNOWN, BEFORE, RUNNING, OVER
}


@Serializable
sealed class MediaType {
    abstract val isMedia: Boolean

    @Serializable
    @SerialName("Photo")
    data class Photo(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("Video")
    data class Video(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("WebRTCFetchConnection")
    data class WebRTCFetchConnection(val url: String, override val isMedia: Boolean = true) : MediaType()

    @Serializable
    @SerialName("WebRTCConnection")
    data class WebRTCConnection(val url: String, val peerName: String, override val isMedia: Boolean = true) :
        MediaType()

    @Serializable
    @SerialName("TaskStatus")
    data class TaskStatus(val teamId: Int) : MediaType() {
        override val isMedia = false
    }

    fun applyTemplate(teamId: String) = when (this) {
        is Photo -> copy(url = url.replace("{teamId}", teamId))
        is Video -> copy(url = url.replace("{teamId}", teamId))
        is WebRTCFetchConnection -> copy(url = url.replace("{teamId}", teamId))
        is WebRTCConnection -> copy(url = url.replace("{teamId}", teamId), peerName = peerName.replace("{teamId}", teamId))
        else -> this
    }

    fun noMedia(): MediaType = when (this) {
        is Photo -> copy(isMedia = false)
        is Video -> copy(isMedia = false)
        is WebRTCFetchConnection -> copy(isMedia = false)
        is WebRTCConnection -> copy(isMedia = false)
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
    val isHidden: Boolean = false,
)

@Serializable
enum class PenaltyRoundingMode {
    @SerialName("each_submission_down_to_minute")
    EACH_SUBMISSION_DOWN_TO_MINUTE,
    @SerialName("sum_down_to_minute")
    SUM_DOWN_TO_MINUTE,
}


@Serializable
data class ContestInfo(
    val status: ContestStatus,
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
    @SerialName("holdBeforeStartTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val holdBeforeStartTime: Duration? = null,
    val emulationSpeed: Double = 1.0,
    val medals: List<MedalType> = emptyList(),
    val penaltyPerWrongAttempt: Int = 20,
    val penaltyRoundingMode: PenaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
) {
    val currentContestTime
        get() = when (status) {
            ContestStatus.BEFORE, ContestStatus.UNKNOWN -> Duration.ZERO
            ContestStatus.RUNNING -> (Clock.System.now() - startTime) * emulationSpeed
            ContestStatus.OVER -> contestLength
        }

    companion object {
        fun unknown() = ContestInfo(
            ContestStatus.UNKNOWN,
            Instant.fromEpochMilliseconds(0),
            Duration.ZERO,
            Duration.ZERO,
            emptyList(),
            emptyList()
        )
    }
}
