@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.ContestInfo as CDSContestInfo
import org.icpclive.cds.ProblemInfo as CDSProblemsInfo
import org.icpclive.cds.RunInfo as CDSRunInfo
import org.icpclive.cds.TeamInfo as CDSTeamInfo

@Serializable
data class Advertisement(val text: String)

@Serializable
data class Picture(val url: String, val name: String)

@Serializable
class QueueSettings() // TODO??

@Serializable
data class RunInfo(
    val id: Int,
    val isAccepted: Boolean,
    val isJudged: Boolean,
    val result: String,
    val problemId: Int,
    val teamId: Int,
    val isReallyUnknown: Boolean,
    val percentage: Double,
    val time: Long,
    val lastUpdateTime: Long,
    val isFirstSolvedRun: Boolean,
) {
    constructor(info: CDSRunInfo) : this(
        info.id,
        info.isAccepted,
        info.isJudged,
        info.result,
        info.problemId,
        info.teamId,
        info.isReallyUnknown,
        info.percentage,
        info.time,
        info.lastUpdateTime,
        info.isFirstSolvedRun
    )
}

fun CDSRunInfo.toApi() = RunInfo(this)

@Serializable
data class ProblemInfo(val letter: String, val name: String, val color: String) {
    constructor(info: CDSProblemsInfo) :
            this(info.letter, info.name, "#" + "%08x".format(info.color.rgb))
}

fun CDSProblemsInfo.toApi() = ProblemInfo(this)

@Serializable
enum class ContestStatus {
    UNKNOWN, BEFORE, RUNNING, PAUSED, OVER
}

@Serializable
data class ContestInfo(
    val status: ContestStatus,
    val startTimeUnixMs: Long,
    val contestLengthMs: Long,
    val freezeTimeMs: Long,
    val problems: List<ProblemInfo>,
    val teams: List<TeamInfo>
) {
    constructor(info: CDSContestInfo) : this(
        info.status,
        info.startTime,
        info.contestLength.toLong(),
        info.freezeTime.toLong(),
        info.problems.map { it.toApi() },
        info.standings.map { it.toApi() }.sortedBy { it.id }
    )

    companion object {
        val EMPTY = ContestInfo(ContestStatus.UNKNOWN, 0, 0, 0, emptyList(), emptyList())
    }
}

fun CDSContestInfo.toApi() = ContestInfo(this)

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
) : ProblemResult()


@Serializable
data class TeamInfo(
    val id: Int,
    val rank: Int,
    val name: String,
    val shortName: String?,
    val alias: String?,
    val groups: List<String>,
    val penalty: Int,
    val solvedProblemsNumber: Int,
    val lastAccepted: Long,
    val hashTag: String?,
) {
    constructor(info: CDSTeamInfo) : this(
        info.id,
        info.rank,
        info.name,
        info.shortName,
        info.alias,
        info.groups.toList(),
        info.penalty,
        info.solvedProblemsNumber,
        info.lastAccepted,
        info.hashTag
    )
}

fun CDSTeamInfo.toApi() = TeamInfo(this)

@Serializable
data class Scoreboard(val rows: List<ScoreboardRow>)

@Serializable
data class ScoreboardRow(
    val teamId: Int,
    val rank: Int,
    val totalScore: Int,
    val penalty: Int,
    val problemResults: List<ProblemResult>,
) {
    constructor(info: CDSTeamInfo) : this(
        info.id,
        info.rank,
        info.solvedProblemsNumber,
        info.penalty,
        parseProblemResults(info.runs)
    )

    companion object {
        // TODO: move it to CDSTeamInfo when it moved to kotlin
        fun parseProblemResults(problemRuns: List<List<CDSRunInfo>>) =
            problemRuns.map { runs ->
                val (runsBeforeFirstOk, okRun) = synchronized(runs) {
                    val okRunIndex = runs.indexOfFirst { it.isAccepted }
                    if (okRunIndex == -1) {
                        runs to null
                    } else {
                        runs.toList().subList(0, okRunIndex) to runs[okRunIndex]
                    }
                }
                ICPCProblemResult(
                    runsBeforeFirstOk.count { it.isAddingPenalty },
                    runsBeforeFirstOk.count { !it.isJudged },
                    okRun != null,
                    okRun?.isFirstSolvedRun == true
                )
            }
    }

}