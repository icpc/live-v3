package org.icpclive.cds.pcms

import org.icpclive.cds.ContestInfo
import org.icpclive.cds.OptimismLevel
import org.icpclive.cds.RunInfo
import org.icpclive.cds.TeamInfo
import kotlin.collections.ArrayList
import kotlin.math.*

class PCMSContestInfo(override val problemsNumber: Int) : ContestInfo() {
    private fun getPossibleStandings(optimistic: Boolean): List<TeamInfo> {
        val original = standings_
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

    override fun getStandings(optimismLevel: OptimismLevel): List<TeamInfo> {
        return when (optimismLevel) {
            OptimismLevel.NORMAL -> standings
            OptimismLevel.OPTIMISTIC -> getPossibleStandings(true)
            OptimismLevel.PESSIMISTIC -> getPossibleStandings(false)
        }
    }

    fun fillTimeFirstSolved() {
        standings_.forEach { teamInfo: PCMSTeamInfo ->
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
        standings_[0].rank = 1
        for (i in 1 until standings_.size) {
            if (TeamInfo.comparator.compare(standings_[i], standings_[i - 1]) == 0) {
                standings_[i].rank = standings_[i - 1].rank
            } else {
                standings_[i].rank = i + 1
            }
        }
    }

    fun makeRuns() {
        val runs = ArrayList<RunInfo>()
        for (team in standings_) {
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
        standings_.add(teamInfo)
        positions[teamInfo.alias] = standings_.size - 1
    }

    override val teamsNumber
        get() = standings_.size

    private fun getParticipant(teamRank: Int?): PCMSTeamInfo? {
        return if (teamRank == null) null else standings_[teamRank]
    }

    override fun getParticipant(name: String): PCMSTeamInfo? {
        val teamRank = getParticipantRankByName(name)
        return getParticipant(teamRank)
    }

    override fun getParticipant(id: Int): PCMSTeamInfo? {
        for (team in standings_) {
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

    override fun getParticipantByHashTag(hashTag: String?): PCMSTeamInfo? {
        for (teamInfo in standings_) {
            if (hashTag != null && hashTag.equals(teamInfo.hashTag, ignoreCase = true)) {
                return teamInfo
            }
        }
        return null
    }

    var standings_: ArrayList<PCMSTeamInfo> = ArrayList()
    override val standings: List<TeamInfo>
        get() = standings_
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