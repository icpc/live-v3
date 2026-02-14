package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
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
public sealed class ProblemResult {
    public abstract val lastSubmitTime: Duration?
}

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
    override val lastSubmitTime: Duration?,
) : ProblemResult()

@Serializable
@SerialName("IOI")
public data class IOIProblemResult(
    val score: Double?,
    @SerialName("lastSubmitTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    override val lastSubmitTime: Duration?,
    val isFirstBest: Boolean,
    val pendingAttempts: Int,
    val totalAttempts: Int,
) : ProblemResult()

@Serializable
public data class ScoreboardRow(
    val totalScore: Double,
    @Serializable(with = DurationInSecondsSerializer::class)
    val penalty: Duration,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("lastAcceptedMs")
    val lastAccepted: Duration,
    val problemResults: List<ProblemResult>,
)

@Serializable
public data class Award(
    public val id: String,
    public val citation: String,
    public val teams: Set<TeamId>,
)

/**
 * @param rows map from teams [TeamInfo.id] to scoreboard row for that team, if that row changed from previous update
 * @param order list of team ids in order of scoreboard
 * @param ranks rank of the corresponding team in order
 * @param awards for each award list of [TeamInfo.id] to receive it.
 */
@Serializable
public data class ScoreboardDiff(
    val rows: Map<TeamId, ScoreboardRow>,
    val order: List<TeamId>,
    val ranks: List<Int>,
    val awards: List<Award>,
)