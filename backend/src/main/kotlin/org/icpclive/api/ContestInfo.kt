package org.icpclive.api

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.utils.ColorSerializer
import org.icpclive.utils.DurationInMillisecondsSerializer
import org.icpclive.utils.UnixMillisecondsSerializer
import org.icpclive.utils.getLogger
import java.awt.Color
import kotlin.time.Duration

@Serializable
data class MedalType(val name: String, val count: Int)

@Serializable
data class ProblemInfo(
    val letter: String,
    val name: String,
    @Serializable(ColorSerializer::class) val color: Color,
    val id: Int,
    val ordinal:Int
) {
    constructor(letter: String, name: String, color: String?, id:Int, ordinal: Int) :
            this(letter, name, parseColor(color) ?: Color.BLACK, id, ordinal)


    companion object {
        fun parseColor(color: String?): Color? = try {
            when {
                color == null -> null
                color.startsWith("0x") -> Color.decode(color)
                color.startsWith("#") -> Color.decode("0x" + color.substring(1))
                else -> Color.decode("0x$color")
            }
        } catch (e: Exception) {
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
enum class MediaType {
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
    val medias: Map<MediaType, String>
)

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
    val emulationSpeed: Double = 1.0,
    val medals: List<MedalType> = emptyList(),
    val penaltyPerWrongAttempt: Int = 20
) {
    val currentContestTime
        get() = when (status) {
            ContestStatus.BEFORE, ContestStatus.UNKNOWN -> Duration.ZERO
            ContestStatus.RUNNING -> (Clock.System.now() - startTime) * emulationSpeed
            ContestStatus.OVER -> contestLength
        }
}