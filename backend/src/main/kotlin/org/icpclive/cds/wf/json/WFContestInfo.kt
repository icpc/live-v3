package org.icpclive.cds.wf.json

import org.icpclive.cds.wf.WFContestInfo
import org.icpclive.cds.wf.WFRunInfo
import org.icpclive.cds.wf.WFTeamInfo

/**
 * Created by Aksenov239 on 3/5/2018.
 */
class WFContestInfo : WFContestInfo() {
    fun initializationFinish() {
        problemsNumber = problems.size
        teamsNumber = teamInfos.size
        wfRuns = mutableListOf()
    }

    // Groups
    var groupById: HashMap<String, String>? = null

    // Problems
    var problemById: HashMap<String, WFProblemInfo>

    // Teams
    var teamById: HashMap<String, WFTeamInfo>

    // Languages
    var languageById: HashMap<String, WFLanguageInfo>

    // Submissions
    var runBySubmissionId: HashMap<String, WFRunInfo>
    var runByJudgementId: HashMap<String, WFRunInfo>

    init {
        problemById = HashMap()
        teamById = HashMap()
        languageById = HashMap()
        runBySubmissionId = HashMap()
        runByJudgementId = HashMap()
    }

    fun addRun(runInfo: WFRunInfo) {
        runInfo.id = wfRuns.size
        wfRuns.add(runInfo)
        teamInfos[runInfo.teamId]!!.addRun(runInfo, runInfo.problemId)
        getProblemById(runInfo.problemId).submissions[runInfo.languageId]++
    }

    fun getTeamByCDSId(cdsId: String): WFTeamInfo? {
        return teamById[cdsId]
    }
}