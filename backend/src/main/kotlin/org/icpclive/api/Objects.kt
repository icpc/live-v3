@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.utils.toHex
import kotlin.time.Duration.Companion.milliseconds
import org.icpclive.cds.ProblemInfo as CDSProblemsInfo


interface TypeWithId {
    val id: String
}

@Serializable
data class ObjectStatus<SettingsType : ObjectSettings>(val shown: Boolean, val settings: SettingsType, val id: Int?)

@Serializable
data class RunInfo(
    val id: Int,
    val isAccepted: Boolean,
    val isJudged: Boolean,
    val isAddingPenalty: Boolean,
    val result: String,
    val problemId: Int,
    val teamId: Int,
    val percentage: Double,
    val time: Long,
    val isFirstSolvedRun: Boolean,
)

@Serializable
data class ProblemInfo(val letter: String, val name: String, val color: String) {
    constructor(info: CDSProblemsInfo) :
            this(info.letter, info.name, info.color.toHex())
}

fun CDSProblemsInfo.toApi() = ProblemInfo(this)

@Serializable
enum class ContestStatus {
    UNKNOWN, BEFORE, RUNNING, OVER
}

@Serializable
data class ContestInfo(
    val status: ContestStatus,
    val startTimeUnixMs: Long,
    val contestLengthMs: Long,
    val freezeTimeMs: Long,
    val problems: List<ProblemInfo>,
    val teams: List<TeamInfo>,
    val emulationSpeed: Double = 1.0,
) {
    val currentContestTime
        get() = when (status) {
            ContestStatus.BEFORE, ContestStatus.UNKNOWN -> 0
            ContestStatus.RUNNING -> ((System.currentTimeMillis() - startTimeUnixMs) * emulationSpeed).toLong()
            ContestStatus.OVER -> contestLengthMs
        }.milliseconds
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
    val lastSubmitTimeMs: Long?,
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
)

@Serializable
data class ProblemSolutionsStatistic(val success: Int, val wrong: Int, val pending: Int)

@Serializable
data class SolutionsStatistic(val stats: List<ProblemSolutionsStatistic>)

@Serializable
data class AdminUser(val login: String, val confirmed: Boolean)

@Serializable
data class TeamInfoOverride(
    val name: String? = null,
    val shortname: String? = null,
    val groups: List<String>? = null,
    val hashTag: String? = null,
    val medias: Map<MediaType, String?>? = null,
)

@Serializable
data class ProblemInfoOverride(
    val name: String? = null,
    val color: String? = null,
)


@Serializable
data class AdvancedProperties(
    val startTime: String? = null,
    val teamOverrides: Map<String, TeamInfoOverride>? = null,
    val problemOverrides: Map<String, ProblemInfoOverride>? = null
)
