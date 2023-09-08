package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.*
import kotlin.time.Duration

@Serializable
public enum class OptimismLevel {
    @SerialName("normal")
    NORMAL,

    @SerialName("optimistic")
    OPTIMISTIC,

    @SerialName("pessimistic")
    PESSIMISTIC;
}

@Serializable
public sealed class ProblemResult

@Serializable
public data class ScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Double,
    @Serializable(with = DurationInSecondsSerializer::class)
    val penalty: Duration,
    val lastAccepted: Long,
    val medalType: String?,
    val problemResults: List<ProblemResult>,
    val teamGroups: List<String>,
    val championInGroups: List<String>
)

//TODO: custom string, maybe something else
@Serializable
@SerialName("ICPC")
public data class ICPCProblemResult(
    val wrongAttempts: Int,
    val pendingAttempts: Int,
    val isSolved: Boolean,
    val isFirstToSolve: Boolean,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
) : ProblemResult()

@Serializable
@SerialName("IOI")
public data class IOIProblemResult(
    val score: Double?,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
    val isFirstBest: Boolean
) : ProblemResult()

@Serializable
public data class Scoreboard(
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration,
    val rows: List<ScoreboardRow>
)
