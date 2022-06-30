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
}