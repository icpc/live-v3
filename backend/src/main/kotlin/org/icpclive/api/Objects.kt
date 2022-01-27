@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.events.ProblemInfo as EventsProblemsInfo
import org.icpclive.events.RunInfo as EventsRunInfo
import org.icpclive.events.TeamInfo as EventsTeamInfo
import org.icpclive.events.ContestInfo as EventsContestInfo

@Serializable
data class Advertisement(val text: String)

@Serializable
data class Picture(val url: String, val name: String)

@Serializable
class QueueSettings() // TODO??

@Serializable
data class RunInfo(
    val id:Int,
    val isAccepted:Boolean,
    val isJudged:Boolean,
    val result:String,
    val problemId:Int,
    val teamId: Int,
    val isReallyUnknown: Boolean,
    val percentage: Double,
    val time: Long,
    val lastUpdateTime: Long,
    val isFirstSolvedRun: Boolean,
) {
    constructor(info: EventsRunInfo): this(
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

fun EventsRunInfo.toApi() = RunInfo(this)

@Serializable
data class ProblemInfo(val letter: String, val name: String, val color: String) {
    constructor(info: EventsProblemsInfo) :
            this(info.letter, info.name, "#" + "%08x".format(info.color.rgb))
}

fun EventsProblemsInfo.toApi() = ProblemInfo(this)

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
    constructor(info: EventsContestInfo) : this(
        info.status,
        info.startTime,
        EventsContestInfo.CONTEST_LENGTH.toLong(),
        EventsContestInfo.FREEZE_TIME.toLong(),
        info.problems.map { it.toApi() },
        info.standings.map { it.toApi() }.sortedWith(compareBy({ it.rank }, { it.name }))
    )
    companion object {
        val EMPTY = ContestInfo(ContestStatus.UNKNOWN, 0, 0, 0, emptyList(), emptyList())
    }
}

fun EventsContestInfo.toApi() = ContestInfo(this)

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
    val problemResults: List<ProblemResult>
) {
    constructor(info: EventsTeamInfo) : this(
        info.id,
        info.rank,
        info.name,
        info.shortName,
        info.alias,
        info.groups.toList(),
        info.penalty,
        info.solvedProblemsNumber,
        info.lastAccepted,
        info.hashTag,
        parseProblemResults(info.runs)
    )

    companion object {
        // TODO: move it to EventsTeamInfo when it moved to kotlin
        fun parseProblemResults(problemRuns: Array<List<EventsRunInfo>>) =
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

fun EventsTeamInfo.toApi() = TeamInfo(this)
