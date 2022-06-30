package org.icpclive.cds.ejudge

import kotlinx.datetime.Instant
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.ProblemInfo
import kotlin.time.Duration

/**
 * @author Mike Perveev
 */
class EjudgeContestInfo(
    val problems: List<ProblemInfo>,
    val teams: Map<String, EjudgeTeamInfo>,
    var startTime: Instant,
    var status: ContestStatus,
    val contestLength: Duration,
    val freezeTime: Duration
) {
    val problemsNumber: Int
        get() = problems.size
    val teamsNumber: Int
        get() = teams.size
    var contestTime: Duration = Duration.ZERO

    fun toApi() = ContestInfo(
        status,
        startTime,
        contestLength,
        freezeTime,
        problems,
        teams.values.map { it.teamInfo }.sortedBy { it.id },
    )

//    fun getParticipant(name: String): EjudgeTeamInfo? {
//        return teams.getOrDefault(name, null)
//    }
//
//    fun getParticipant(id: Int): EjudgeTeamInfo? {
//        return teams.
//        return teams.firstOrNull { it.id == id }
//    }
//
//    fun getParticipantByHashTag(hashTag: String): EjudgeTeamInfo? {
//        return teams.firstOrNull { it.hashTag == hashTag }
//    }
//
    fun getParticipantByContestSystemId(contestSystemId: Int): EjudgeTeamInfo? {
        return teams[contestSystemId.toString()]
    }
}