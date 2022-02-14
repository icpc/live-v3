package org.icpclive.cds.wf

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.api.ScoreboardRow
import org.icpclive.cds.*
import org.icpclive.cds.wf.json.WFProblemInfo
import java.util.*
import kotlin.math.max

/**
 * Created by aksenov on 05.05.2015.
 */
open class WFContestInfo : ContestInfo {
    override val problems: MutableList<WFProblemInfo> = mutableListOf()
    override val teams: List<TeamInfo>
        get() = teamInfos.mapNotNull { it }
    protected lateinit var wfRuns: MutableList<WFRunInfo>
    lateinit var languages: Array<String?>
    lateinit var teamInfos: Array<WFTeamInfo?>
    protected lateinit var timeFirstSolved: LongArray
    override lateinit var firstSolvedRun: MutableList<WFRunInfo?>
    var standings: List<WFTeamInfo> = emptyList()
    override var problemsNumber: Int = 0
    override var teamsNumber: Int = 0

    constructor(problemsNumber: Int, teamsNumber: Int) : super(Instant.fromEpochMilliseconds(0), ContestStatus.BEFORE) {
        this.problemsNumber = problemsNumber
        this.teamsNumber = teamsNumber
        teamInfos = arrayOfNulls(teamsNumber)
        timeFirstSolved = LongArray(problemsNumber)
        languages = arrayOfNulls(100)
        wfRuns = mutableListOf()
        firstSolvedRun = MutableList(problemsNumber) { null }
    }

    protected constructor() : super(Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN)

    fun recalcStandings() {
        var n = 0
        Arrays.fill(timeFirstSolved, Long.MAX_VALUE)
        firstSolvedRun.fill(null)
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
        this.standings = standings
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

    /*open fun addRun(run: WFRunInfo) {
//		System.err.println("add runId: " + run.getId());
        if (!runExists(run.id)) {
            wfRuns[run.id] = run
            teamInfos[run.teamId]!!.addRun(run, run.problemId)
        }
    }*/

    fun addTest(test: WFTestCaseInfo) {
        val run = wfRuns.getOrNull(test.runId) ?: return
        run.add(test)
        if (!run.isJudged) {
            run.lastUpdateTime = max(run.lastUpdateTime, test.time)
        }
    }

    override fun getParticipant(name: String): TeamInfo? {
        return teams.firstOrNull { it.name == name || it.shortName == name }
    }

    override fun getParticipant(id: Int): TeamInfo? {
        return teamInfos[id]
    }


    override fun firstTimeSolved(): LongArray? {
        return timeFirstSolved
    }


    val runs: List<WFRunInfo>
        get() = wfRuns

    fun getProblemById(id: Int): WFProblemInfo {
        return problems[id]
    }

    override fun getParticipantByHashTag(hashTag: String): WFTeamInfo? {
        return teamInfos.firstOrNull { hashTag.equals(it?.hashTag, ignoreCase = true) }
    }


    override fun getStandings(optimismLevel: OptimismLevel) =
        ICPCTools.getScoreboard(teams, optimismLevel)

    override val contestTime= TODO()
}