package org.icpclive.cds.ejudge

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.ProblemInfo
import org.icpclive.cds.TeamInfo
import kotlin.time.seconds

/**
 * @author Mike Perveev
 */
class EjudgeContestInfo(
    override val problems: List<ProblemInfo>,
    override val teams: List<EjudgeTeamInfo>,
    startTime: Instant,
    status: ContestStatus
) : ContestInfo(startTime, status) {
    override val problemsNumber: Int
        get() = problems.size
    override val teamsNumber: Int
        get() = teams.size

    override var contestTime = 0.seconds

    override fun getParticipant(name: String): EjudgeTeamInfo? {
        return teams.firstOrNull { it.name == name }
    }

    override fun getParticipant(id: Int): EjudgeTeamInfo? {
        return teams.firstOrNull { it.id == id }
    }

    override fun getParticipantByHashTag(hashTag: String): EjudgeTeamInfo? {
        return teams.firstOrNull { it.hashTag == hashTag }
    }

    public fun getParticipantByContestSystemId(contestSystemId: Int): EjudgeTeamInfo? {
        return teams.firstOrNull { it.contestSystemId == contestSystemId.toString() }
    }
}