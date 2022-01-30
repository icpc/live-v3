package org.icpclive.events.WF.json

import com.google.gson.Gson
import com.google.gson.JsonArray
import org.icpclive.events.NetworkUtils.openAuthorizedStream
import org.icpclive.events.TeamInfo
import org.icpclive.events.WF.WFContestInfo
import org.icpclive.events.WF.WFRunInfo
import org.icpclive.events.WF.WFTeamInfo
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by Aksenov239 on 3/5/2018.
 */
class WFContestInfo : WFContestInfo() {
    fun initializationFinish() {
        problemsNumber = problems.size
        teamsNumber = teamInfos.size
        timeFirstSolved = LongArray(problemsNumber)
        wfRuns = mutableListOf()
        firstSolvedRun = MutableList(problemsNumber) { null }
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