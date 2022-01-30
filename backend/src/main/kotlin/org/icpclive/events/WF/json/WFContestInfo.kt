package org.icpclive.events.WF.json

import org.icpclive.events.NetworkUtils.openAuthorizedStream
import org.icpclive.events.WF.WFRunInfo
import java.util.HashMap
import java.io.BufferedReader
import com.google.gson.Gson
import com.google.gson.JsonArray
import org.icpclive.events.TeamInfo
import org.icpclive.events.WF.WFContestInfo
import org.icpclive.events.WF.WFTeamInfo
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
        wfRuns = arrayOfNulls(1000000)
        firstSolvedRun = arrayOfNulls(problemsNumber)
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

    override fun addRun(runInfo: WFRunInfo) {
        runInfo.id = lastRunId + 1
        wfRuns[runInfo.id] = runInfo
        teamInfos[runInfo.teamId]!!.addRun(runInfo, runInfo.problemId)
        getProblemById(runInfo.problemId).submissions[runInfo.languageId]++
        lastRunId++
    }

    fun getTeamByCDSId(cdsId: String): WFTeamInfo? {
        return teamById[cdsId]
    }

    fun checkStandings(url: String, login: String?, password: String?) {
        try {
            val standings: Array<out TeamInfo> = standings
            val br = BufferedReader(
                InputStreamReader(
                    openAuthorizedStream("$url/scoreboard", login, password!!)
                )
            )
            var json = ""
            var line: String
            while (br.readLine().also { line = it } != null) {
                json += line.trim { it <= ' ' }
            }
            val jsonTeams = Gson().fromJson(json, JsonArray::class.java)
            for (i in 0 until jsonTeams.size()) {
                val je = jsonTeams[i].asJsonObject
                val id = je["team_id"].asString
                val score = je["score"].asJsonObject
                val num_solved = score["num_solved"].asInt
                val total_time = score["total_time"].asInt
                val team: TeamInfo? = getTeamByCDSId(id)
                if (team!!.solvedProblemsNumber != num_solved ||
                    team.penalty != total_time
                ) {
                    System.err.println("Incorrect for team $team")
                    return
                }
            }
            System.err.println("Correct scoreboard")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}