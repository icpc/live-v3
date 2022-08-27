@file:Suppress("unused")

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


interface TypeWithId {
    val id: String
}

@Serializable
data class ObjectStatus<SettingsType : ObjectSettings>(
    val shown: Boolean,
    val settings: SettingsType,
    val id: Int? = null
)

@Serializable
data class RunInfo constructor(
    val id: Int,
    val isAccepted: Boolean,
    val isJudged: Boolean,
    val isAddingPenalty: Boolean,
    val result: String,
    val problemId: Int,
    val teamId: Int,
    val percentage: Double,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val isFirstSolvedRun: Boolean = false,
    val featuredRunMedia: MediaType? = null,
)

@Serializable
data class ProblemInfo(val letter: String, val name: String, @Serializable(ColorSerializer::class) val color: Color) {
    constructor(letter: String, name: String, color: String?) : this(letter, name, parseColor(color) ?: Color.BLACK)

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
data class MedalType(val name: String, val count: Int)

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

@Serializable
sealed class ProblemResult

//TODO: custom string, problem with score, maybe something else
@Serializable
@SerialName("icpc")
data class ICPCProblemResult(
    val wrongAttempts: Int,
    val pendingAttempts: Int,
    val isSolved: Boolean,
    val isFirstToSolve: Boolean,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
) : ProblemResult()


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
data class Scoreboard(val rows: List<ScoreboardRow>)

@Serializable
data class ScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Int,
    val penalty: Int,
    val lastAccepted: Long,
    val medalType: String?,
    val problemResults: List<ProblemResult>,
    val teamGroups: List<String>,
)

@Serializable
data class ProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int)

@Serializable
data class SolutionsStatistic(val stats: List<ProblemSolutionsStatistic>)

@Serializable
data class AnalyticsCompanionPreset(
    val presetId: Int,
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
)

@Serializable
data class AnalyticsCompanionRun(
    @SerialName("expirationTimeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    val expirationTime: Instant?,
    val mediaType: MediaType,
)

@Serializable
sealed class AnalyticsMessage {
    abstract val id: String
    abstract val time: Instant
    abstract val relativeTime: Duration
}

@Serializable
@SerialName("commentary")
data class AnalyticsCommentaryEvent(
    override val id: String,
    val message: String,
    @SerialName("timeUnixMs")
    @Serializable(with = UnixMillisecondsSerializer::class)
    override val time: Instant,
    @SerialName("relativeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    override val relativeTime: Duration,
    val teamIds: List<Int>,
    val runIds: List<Int>,
    val advertisement: AnalyticsCompanionPreset? = null,
    val tickerMessage: AnalyticsCompanionPreset? = null,
    val featuredRun: AnalyticsCompanionRun? = null,
) : AnalyticsMessage()

@Serializable
data class AdminUser(val login: String, val confirmed: Boolean)
