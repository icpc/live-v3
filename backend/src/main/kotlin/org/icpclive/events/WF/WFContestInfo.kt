package org.icpclive.events.WF

import org.icpclive.events.ContestInfo
import java.util.Arrays
import org.icpclive.events.WF.json.WFProblemInfo
import org.icpclive.events.OptimismLevel
import org.icpclive.events.RunInfo
import org.icpclive.events.TeamInfo

/**
 * Created by aksenov on 05.05.2015.
 */
open class WFContestInfo : ContestInfo {
    protected lateinit var wfRuns: Array<WFRunInfo?>
    lateinit var languages: Array<String?>
    lateinit var teamInfos: Array<WFTeamInfo?>
    protected lateinit var timeFirstSolved: LongArray
    override var lastRunId = 0
        protected set
    override lateinit var firstSolvedRun: Array<WFRunInfo?>
    override var standings: Array<WFTeamInfo> = emptyArray()

    constructor(problemsNumber: Int, teamsNumber: Int) {
        this.problemsNumber = problemsNumber
        this.teamsNumber = teamsNumber
        teamInfos = arrayOfNulls(teamsNumber)
        timeFirstSolved = LongArray(problemsNumber)
        languages = arrayOfNulls(100)
        wfRuns = arrayOfNulls(1000000)
        firstSolvedRun = arrayOfNulls(problemsNumber)
    }

    protected constructor() {}

    fun recalcStandings() {
        var n = 0
        Arrays.fill(timeFirstSolved, Long.MAX_VALUE)
        Arrays.fill(firstSolvedRun, null)
        val standings = teamInfos.mapNotNull { team ->
            if (team == null) return@mapNotNull null
            team.solvedProblemsNumber = 0
            team.penalty = 0
            team.lastAccepted = 0
            for (j in 0 until problemsNumber) {
                val runs = team.runs[j]
                var wrong = 0
                for (run in runs) {
                    val wfrun = run as WFRunInfo
                    if ("AC" == run.result) {
                        if (!run.isJudged) {
                            System.err.println("!!!")
                        }
                        team.solvedProblemsNumber++
                        val time = (wfrun.time / 60000).toInt()
                        team.penalty += wrong * 20 + time
                        team.lastAccepted = Math.max(team.lastAccepted, wfrun.time)
                        if (wfrun.time < timeFirstSolved[j]) {
                            timeFirstSolved[j] = wfrun.time
                            firstSolvedRun[j] = wfrun
                        }
                        break
                    } else if (wfrun.result.length > 0 && "CE" != wfrun.result) {
                        wrong++
                    }
                }
            }
            team
        }.toMutableList()
        standings.sortWith(TeamInfo.comparator)
        for (i in 0 until n) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
                standings[i].rank = standings[i - 1].rank
            } else {
                standings[i].rank = i + 1
            }
        }
        this.standings = standings.toTypedArray()
    }

    private fun recalcStandings(standings: MutableList<WFTeamInfo>) {
        for (team in standings) {
            team.solvedProblemsNumber = 0
            team.penalty = 0
            team.lastAccepted = 0
            for (j in 0 until problemsNumber) {
                val runs = team.runs[j]
                var wrong = 0
                for (run in runs) {
                    if ("AC" == run.result) {
                        team.solvedProblemsNumber++
                        val time = (run.time / 60000).toInt()
                        team.penalty += wrong * 20 + time
                        team.lastAccepted = Math.max(team.lastAccepted, run.time)
                        break
                    } else if (run.result.length > 0 && "CE" != run.result) {
                        wrong++
                    }
                }
            }
        }
        standings.sortWith(TeamInfo.strictComparator)
        for (i in standings.indices) {
            if (i > 0 && TeamInfo.comparator.compare(standings[i], standings[i - 1]) == 0) {
                standings[i].rank = standings[i - 1].rank
            } else {
                standings[i].rank = i + 1
            }
        }
    }

    fun addTeam(team: WFTeamInfo) {
        teamInfos[team.id] = team
    }

    fun runExists(id: Int): Boolean {
        return wfRuns[id] != null
    }

    override fun getRun(id: Int): WFRunInfo? {
        return wfRuns[id]
    }

    open fun addRun(run: WFRunInfo) {
//		System.err.println("add runId: " + run.getId());
        if (!runExists(run.id)) {
            wfRuns[run.id] = run
            teamInfos[run.teamId]!!.addRun(run, run.problemId)
            lastRunId = Math.max(lastRunId, run.id)
        }
    }

    fun addTest(test: WFTestCaseInfo) {
//		System.out.println("Adding test " + test.id + " to runId " + test.runId);
        if (runExists(test.runId)) {
            val run = wfRuns[test.runId]
            run!!.add(test)
            if (!run.isJudged) {
                run.lastUpdateTime = Math.max(run.lastUpdateTime, test.time)
            }
            //			System.out.println("Run " + runs[test.runId] + " passed " + runs[test.runId].getPassedTestsNumber() + " tests");
        }
    }

    override fun getParticipant(name: String?): TeamInfo? {
        for (i in 0 until teamsNumber) {
            if (teamInfos[i]!!.name == name || teamInfos[i]!!.shortName == name) {
                return teamInfos[i]
            }
        }
        return null
    }

    override fun getParticipant(id: Int): TeamInfo? {
        return teamInfos[id]
    }


    override fun firstTimeSolved(): LongArray? {
        return timeFirstSolved
    }


    override val runs: Array<WFRunInfo?>
        get() = wfRuns

    fun getProblemById(id: Int): WFProblemInfo {
        return problems[id] as WFProblemInfo
    }

    override fun getParticipantByHashTag(hashTag: String?): WFTeamInfo? {
        for (i in 0 until teamsNumber) {
            if (hashTag != null && hashTag.equals(teamInfos[i]!!.hashTag, ignoreCase = true)) {
                return teamInfos[i]
            }
        }
        return null
    }

    private fun getPossibleStandings(isOptimistic: Boolean): Array<out TeamInfo> {
        val possibleStandings = standings.map { team ->
            team.copy().apply {
                for (j in 0 until problemsNumber) {
                    val runs = team.runs[j]
                    var runIndex = 0
                    for (run in runs) {
                        val clonedRun = WFRunInfo((run as WFRunInfo))
                        if (clonedRun.result.length == 0) {
                            clonedRun.isJudged = true
                            val expectedResult = if (isOptimistic) "AC" else "WA"
                            clonedRun.result = if (runIndex == runs.size - 1) expectedResult else "WA"
                            clonedRun.isReallyUnknown = true
                        }
                        addRun(clonedRun, j)
                        runIndex++
                    }
                }
            }
        }.toMutableList()
        recalcStandings(possibleStandings)
        return possibleStandings.toTypedArray()
    }

    override fun getStandings(level: OptimismLevel): Array<out TeamInfo> {
        return when (level) {
            OptimismLevel.NORMAL -> standings
            OptimismLevel.OPTIMISTIC -> getPossibleStandings(true)
            OptimismLevel.PESSIMISTIC -> getPossibleStandings(false)
        }
    }
}