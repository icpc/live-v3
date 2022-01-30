package org.icpclive.cds.pcms

import org.icpclive.cds.*
import kotlin.collections.ArrayList
import kotlin.math.*

class PCMSContestInfo(override val problemsNumber: Int) : ContestInfo() {
    override val problems : MutableList<ProblemInfo> = mutableListOf()
    override val teams: List<TeamInfo>
        get() = standings
    private fun getPossibleStandings(optimistic: Boolean): List<TeamInfo> {
        val original = standings
        val standings = original.indices.map { i ->
            val row = original[i].copy()
            val runs = original[i].runs
            for (j in 0 until problemsNumber) {
                var runIndex = 0
                for (run in runs[j]) {
                    val clonedRun = PCMSRunInfo(run)
                    if (!clonedRun.isJudged) {
                        clonedRun.isJudged = true
                        val expectedResult = if (optimistic) "AC" else "WA"
                        clonedRun.result = if (runIndex == runs[j].size - 1) expectedResult else "WA"
                        clonedRun.isReallyUnknown = true
                    }
                    row.addRun(clonedRun, j)
                    runIndex++
                }
            }
            row
        }.toMutableList()
        for (team in standings) {
            team.solvedProblemsNumber = 0
            team.penalty = 0
            team.lastAccepted = 0
            val runs = team.runs
            for (j in 0 until problemsNumber) {
                var wrong = 0
                for (run in runs[j]) {
                    if ("AC" == run.result) {
                        team.solvedProblemsNumber++
                        val time = (run.time / 60 / 1000).toInt()
                        team.penalty += wrong * 20 + time
                        team.lastAccepted = max(team.lastAccepted, run.time)
                        break
                    } else if (run.result.length > 0 && "CE" != run.result) {
                        wrong++
                    }
                }
            }
        }
        standings.sortWith(TeamInfo.comparator)
        for (i in standings.indices) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i - 1], standings[i]) == 0) {
                standings[i].rank = standings[i - 1].rank
            } else {
                standings[i].rank = i + 1
            }
        }
        return standings
    }

    override fun getStandings(optimismLevel: OptimismLevel) =
        when (optimismLevel) {
            OptimismLevel.NORMAL -> standings
            OptimismLevel.OPTIMISTIC -> getPossibleStandings(true)
            OptimismLevel.PESSIMISTIC -> getPossibleStandings(false)
        }.map { it.apiScoreboardRow }

    fun fillTimeFirstSolved() {
        standings.forEach { teamInfo: PCMSTeamInfo ->
            val runs = teamInfo.runs
            for (i in runs.indices) {
                for (run in runs[i]) {
                    if (run.isAccepted) {
                        timeFirstSolved[i] = min(timeFirstSolved[i], run.time)
                    }
                }
            }
        }
    }

    fun calculateRanks() {
        standings[0].rank = 1
        for (i in 1 until standings.size) {
            if (TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
                standings[i].rank = standings[i - 1].rank
            } else {
                standings[i].rank = i + 1
            }
        }
    }

    fun makeRuns() {
        val runs = ArrayList<RunInfo>()
        for (team in standings) {
            for (innerRuns in team.runs) {
                runs.addAll(innerRuns)
            }
        }
        this.runs = runs
        this.runs.sort()
        for (run in this.runs) {
            if (firstSolvedRuns[run.problemId] == null && run.isAccepted && run.time <= freezeTime) {
                firstSolvedRuns[run.problemId] = run as PCMSRunInfo
            }
        }
    }

    fun addTeamStandings(teamInfo: PCMSTeamInfo) {
        standings.add(teamInfo)
        positions[teamInfo.alias] = standings.size - 1
    }

    override val teamsNumber
        get() = standings.size

    private fun getParticipant(teamRank: Int?): PCMSTeamInfo? {
        return if (teamRank == null) null else standings[teamRank]
    }

    override fun getParticipant(name: String): PCMSTeamInfo? {
        val teamRank = getParticipantRankByName(name)
        return getParticipant(teamRank)
    }

    override fun getParticipant(id: Int): PCMSTeamInfo? {
        for (team in standings) {
            if (team.id == id) {
                return team
            }
        }
        return null
    }

    fun getParticipantRankByName(participantName: String): Int? {
        return positions[participantName]
    }

    override fun firstTimeSolved(): LongArray {
        return timeFirstSolved
    }

    override fun getParticipantByHashTag(hashTag: String): PCMSTeamInfo? {
        for (teamInfo in standings) {
            if (hashTag != null && hashTag.equals(teamInfo.hashTag, ignoreCase = true)) {
                return teamInfo
            }
        }
        return null
    }

    var standings: ArrayList<PCMSTeamInfo> = ArrayList()
    override val firstSolvedRun: List<RunInfo?>
        get() = firstSolvedRuns
    var timeFirstSolved = LongArray(problemsNumber)
    var positions: MutableMap<String, Int> = mutableMapOf()
    var frozen = false
    override var runs: MutableList<RunInfo> = mutableListOf()
    private var firstSolvedRuns: MutableList<PCMSRunInfo?> = MutableList(problemsNumber) { null }

    init {
        freezeTime = 4 * 60 * 60 * 1000
    }
}