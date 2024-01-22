package org.icpclive.cds.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import org.icpclive.util.DurationInSecondsSerializer
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

@Serializable
public data class LegacyScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Double,
    @Serializable(with = DurationInSecondsSerializer::class)
    val penalty: Duration,
    val lastAccepted: Long,
    val medalType: String?,
    val problemResults: List<ProblemResult>,
    val teamGroups: List<String>,
    val championInGroups: List<String>,
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
) : ProblemResult()

@Serializable
public data class LegacyScoreboard(
    val rows: List<LegacyScoreboardRow>,
)

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
public sealed class Award {
    public abstract val id: String
    public abstract val citation: String
    public abstract val teams: Set<Int>

    @Serializable
    @SerialName("winner")
    public data class Winner(
        override val id: String,
        override val citation: String,
        override val teams: Set<Int>,
    ) : Award()

    @Serializable
    @SerialName("medal")
    public data class Medal(
        override val id: String,
        override val citation: String,
        val medalColor: MedalColor?,
        override val teams: Set<Int>,
    ) : Award() {
        public enum class MedalColor {
            GOLD, SILVER, BRONZE;
        }
    }

    @Serializable
    @SerialName("group_champion")
    public data class GroupChampion(
        override val id: String,
        override val citation: String,
        val groupId: String,
        override val teams: Set<Int>,
    ) : Award()

    @Serializable
    @SerialName("custom")
    public data class Custom(
        override val id: String,
        override val citation: String,
        override val teams: Set<Int>,
    ) : Award()
}

public enum class ScoreboardUpdateType {
    DIFF,
    SNAPSHOT
}


/**
 * @param type if [ScoreboardUpdateType.SNAPSHOT] all teams not in rows should be dropped, if [ScoreboardUpdateType.DIFF], only mentioned teams should be replaced
 *        Other fields are always fully transferred
 * @param rows map from teams [TeamInfo.id] to scoreboard row for that team
 * @param order list of team ids in order of scoreboard
 * @param ranks rank of the corresponding team in order
 * @param awards for each award list of [TeamInfo.id] to receive it.
 */
@Serializable
public data class Scoreboard(
    val type: ScoreboardUpdateType,
    val rows: Map<Int, ScoreboardRow>,
    val order: List<Int>,
    val ranks: List<Int>,
    val awards: List<Award>,
)