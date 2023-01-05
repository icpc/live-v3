package org.icpclive.cds.cats

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoaderService
import org.icpclive.util.getCredentials
import java.awt.Color
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


class CATSDataSource(val properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)

    // :TODO I may send less count of quires to the server but I need to know when my sid is expired
    private val sid: String
        get() = runBlocking {
            async { authLoader.loadOnce().sid }.await()
        }


    @Serializable
    data class Auth(val status: String, val sid: String, val cid: Long)

    @Serializable
    data class Problem(val id: Int, val name: String, val code: String)

    @Serializable
    data class Problems(val problems: ArrayList<Problem>)

    @Serializable
    data class Team(val id: Int, val login: String, val name: String, val role: String)

    @Serializable
    data class Users(val users: ArrayList<Team>)

    @Serializable
    data class Contest(
        val title: String,
        val start_date: String,
        val freeze_date: String,
        val finish_date: String,
        val rules: String
    )

    @Serializable
    data class Run(
        val type: String,
        val id: Int? = null,
        val state_text: String? = null,
        val problem_id: Int? = null,
        val team_id: Int? = null,
        val time: String? = null
        // ::TODO add failed tests
    )

    private val authLoader =
        jsonLoaderService<Auth> { "${properties.getProperty("url")}?f=login;login=$login;passwd=$password;json=1" }
    private val problemsLoader =
        jsonLoaderService<Problems> { "${properties.getProperty("url")}problems?cid=${properties.getProperty("cid")};sid=$sid;json=1" }
    private val usersLoader =
        jsonLoaderService<Users> { "${properties.getProperty("url")}users?cid=${properties.getProperty("cid")};sid=$sid;rows=1000;json=1" }
    private val contestLoader =
        jsonLoaderService<Contest> { "${properties.getProperty("url")}contest_params?cid=${properties.getProperty("cid")};sid=$sid;json=1" }
    private val runsLoader =
        jsonLoaderService<ArrayList<Run>> { "${properties.getProperty("url")}console?cid=${properties.getProperty("cid")};sid=$sid;rows=1000;json=1" }

    override suspend fun loadOnce(): ContestParseResult {
//        val element = problemsLoader.loadOnce()
//        for (elem in element.problems) {
//            println(elem)
//        }
//
//        val contestInfo = ContestInfo(
//            status = ContestStatus.UNKNOWN,
//            resultType = ContestResultType.ICPC,
//            startTime = Clock.System.now(),
//            contestLength = 5.hours,
//            freezeTime = 1.hours,
//            problems = listOf(),
//            teams = listOf()
//        )
//        return ContestParseResult(contestInfo, listOf(), listOf())
        return parseAndUpdateStandings(problemsLoader.loadOnce(), usersLoader.loadOnce(), contestLoader.loadOnce(), runsLoader.loadOnce())
    }

    private fun parseAndUpdateStandings(problems: Problems, users: Users, contest: Contest, runs: ArrayList<Run>): ContestParseResult {
        val problemsList = ArrayList<ProblemInfo>()
        for (i in 0 until problems.problems.size) {
            val problem = problems.problems[i]
            problemsList.add(
                ProblemInfo(
                    letter = problem.code,
                    name = problem.name,
                    color = Color(0x000000),
                    id = problem.id,
                    ordinal = i
                )
            )
        }

        val teamList = ArrayList<TeamInfo>()
        for (i in 0 until users.users.size) {
            val team = users.users[i];
            if (team.role != "in_contest") {
                continue;
            }
            teamList.add(
                TeamInfo(
                    id = team.id,
                    name = team.name,
                    shortName = team.name,
                    contestSystemId = team.login,
                    groups = listOf(),
                    hashTag = null,
                    medias = mapOf()
                )
            )
        }
        // TODO: Add time.
//        val startDate = LocalDateTime.parse(contest.start_date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))


        val contestInfo = ContestInfo(
            status = ContestStatus.OVER,
            resultType = if (contest.rules == "icpc") ContestResultType.ICPC else ContestResultType.IOI,
            // TODO: Add correct time
            startTime = Clock.System.now(),
            contestLength = 5.hours,
            freezeTime = 1.hours,
            problems = problemsList,
            teams = teamList
        )

        val preparedRuns = runs.filter { it.type == "submit" }

        val resultRuns = preparedRuns.map {
            RunInfo(
                id = it.id ?: 0,
                isAccepted = ("AC" == it.state_text),
                isJudged = "" != it.state_text,
                isAddingPenalty = "AC" != it.state_text && "CE" != it.state_text,
                result = it.state_text ?: "",
                problemId = it.problem_id ?: 0,
                teamId = it.team_id ?: 0,
                percentage = if ("" == it.state_text) 0.0 else 1.0,
                time = 0.seconds
            )
        }
        return ContestParseResult(
            contestInfo,
            resultRuns,
            emptyList()
        )
    }
}