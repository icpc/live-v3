package org.icpclive.cds.pcms

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.*
import kotlin.time.Duration.Companion.seconds

class PCMSContestInfo(
    override val problems: List<ProblemInfo>,
    override val teams: List<PCMSTeamInfo>,
    startTime: Instant,
    status: ContestStatus,
    ) : ContestInfo(startTime, status) {
    override val problemsNumber: Int
        get() = problems.size
    override var contestTime = 0.seconds

    override fun getStandings(optimismLevel: OptimismLevel) =
        ICPCTools.getScoreboard(teams, optimismLevel)


    fun calculateRanks() {
        teams[0].rank = 1
        for (i in 1 until teams.size) {
            if (TeamInfo.comparator.compare(teams[i], teams[i - 1]) == 0) {
                teams[i].rank = teams[i - 1].rank
            } else {
                teams[i].rank = i + 1
            }
        }
    }

    override val teamsNumber
        get() = teams.size

    override fun getParticipant(name: String): PCMSTeamInfo? {
        return teams.firstOrNull { it.alias == name }
    }

    override fun getParticipant(id: Int): PCMSTeamInfo? {
        return teams.firstOrNull { it.id == id }
    }

    override fun firstTimeSolved(): LongArray {
        return LongArray(firstSolvedRun.size) { firstSolvedRun[it]?.time ?: 0L }
    }

    override fun getParticipantByHashTag(hashTag: String): PCMSTeamInfo? {
        return teams.firstOrNull { hashTag.equals(it.hashTag, ignoreCase = true)}
    }

    override var firstSolvedRun: MutableList<PCMSRunInfo?> = MutableList(problemsNumber) { null }
}