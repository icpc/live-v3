package org.icpclive.cds.pcms

import org.icpclive.cds.*

class PCMSContestInfo(
    override val problems: List<ProblemInfo>,
    override val teams: List<PCMSTeamInfo>,
    ) : ContestInfo() {
    override val problemsNumber: Int
        get() = problems.size
    override var contestTime: Long = 0

    override fun getStandings(optimismLevel: OptimismLevel) =
        MutableList(teams.size) {
            teams[it].getTeamScoreboardRow(optimismLevel)
        }.apply { sortAndSetRanks(this, teams) }.toList()


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

    fun makeRuns() {
        this.runs = teams.flatMap { it.runs.flatten() }.sortedWith(compareBy(
            { it.time },
            { it.id }
        ))
        for (run in this.runs) {
            if (firstSolvedRun[run.problemId] == null && run.isAccepted) {
                firstSolvedRun[run.problemId] = run
                run.isFirstSolvedRun = true
            } else {
                run.isFirstSolvedRun = false
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

    override var runs: List<PCMSRunInfo> = mutableListOf()
    override var firstSolvedRun: MutableList<PCMSRunInfo?> = MutableList(problemsNumber) { null }
}