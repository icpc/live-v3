@file:Suppress("unused")

package org.icpclive.api

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
    BEFORE, RUNNING, PAUSED, OVER
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
        info.standings.map { it.toApi() }.sortedBy { it.id }
    )
    companion object {
        val EMPTY = ContestInfo(ContestStatus.BEFORE, 0, 0, 0, emptyList(), emptyList())
    }
}

fun EventsContestInfo.toApi() = ContestInfo(this)

@Serializable
data class TeamInfo(
    val id: Int,
    val rank: Int,
    val name: String,
    val shortName: String?,
    val alias: String?,
    val groups: Set<String?>?,
    val penalty: Int,
    val solvedProblemsNumber: Int,
    val lastAccepted: Long,
    val hashTag: String?,
) {
    constructor(info: EventsTeamInfo) : this(
        info.id,
        info.rank,
        info.name,
        info.shortName,
        info.alias,
        info.groups,
        info.penalty,
        info.solvedProblemsNumber,
        info.lastAccepted,
        info.hashTag
    )
}

fun EventsTeamInfo.toApi() = TeamInfo(this)
