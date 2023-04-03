package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
enum class OptimismLevel {
    @SerialName("normal")
    NORMAL,

    @SerialName("optimistic")
    OPTIMISTIC,

    @SerialName("pessimistic")
    PESSIMISTIC;
}

@Serializable
sealed class ProblemResult

@Serializable
data class ScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Double,
    val penalty: Long,
    val lastAccepted: Long,
    val medalType: String?,
    val problemResults: List<ProblemResult>,
    val teamGroups: List<String>,
    val championInGroups: List<String>
)

//TODO: custom string, maybe something else
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
@SerialName("ioi")
data class IOIProblemResult(
    val score: Double?,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
    val isFirstBest: Boolean
) : ProblemResult()

@Serializable
data class Scoreboard(val rows: List<ScoreboardRow>)
