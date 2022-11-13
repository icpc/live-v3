package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
sealed class ProblemResult

@Serializable
data class ScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Int,
    val penalty: Long,
    val lastAccepted: Long,
    val medalType: String?,
    val problemResults: List<ProblemResult>,
    val teamGroups: List<String>,
    val championInGroups: List<String>
)

//TODO: custom string, problem with score, maybe something else
@Serializable
@SerialName("icpc_binary")
data class ICPCBinaryProblemResult(
    val wrongAttempts: Int,
    val pendingAttempts: Int,
    val isSolved: Boolean,
    val isFirstToSolve: Boolean,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
) : ProblemResult()

@Serializable
@SerialName("icpc_score")
data class ICPCScoreProblemResult(
    val wrongAttempts: Int,
    val pendingAttempts: Int,
    val score: Int,
    val isFirstToSolve: Boolean,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val lastSubmitTime: Duration?,
) : ProblemResult()

@Serializable
data class Scoreboard(val rows: List<ScoreboardRow>)
