package org.icpclive.cds.wf

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.wf.json.WFProblemInfo
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
    override var problemsNumber: Int = 0
    override var teamsNumber: Int = 0

    constructor(problemsNumber: Int, teamsNumber: Int) : super(Instant.fromEpochMilliseconds(0), ContestStatus.BEFORE) {
        this.problemsNumber = problemsNumber
        this.teamsNumber = teamsNumber
        teamInfos = arrayOfNulls(teamsNumber)
        languages = arrayOfNulls(100)
        wfRuns = mutableListOf()
    }

    protected constructor() : super(Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN)

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


    val runs: List<WFRunInfo>
        get() = wfRuns

    fun getProblemById(id: Int): WFProblemInfo {
        return problems[id]
    }

    override fun getParticipantByHashTag(hashTag: String): WFTeamInfo? {
        return teamInfos.firstOrNull { hashTag.equals(it?.hashTag, ignoreCase = true) }
    }


    override val contestTime = minOf(Clock.System.now() - startTime, contestLength) // todo: here was TODO()
}
