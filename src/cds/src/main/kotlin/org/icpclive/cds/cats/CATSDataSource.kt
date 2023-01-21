package org.icpclive.cds.cats

import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoaderService
import org.icpclive.util.getCredentials
import org.icpclive.util.getLogger
import java.awt.Color
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds


class CATSDataSource(val properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)
    private val url = properties.getProperty("url")
    private val timezone: String = properties.getProperty("timezone")
    private val resultType: ContestResultType = ContestResultType.valueOf(
        properties.getProperty("standings.resultType")?.toString()?.uppercase() ?: "ICPC"
    )

    private val logger = getLogger(CATSDataSource::class)

    // :TODO I may send less count of quires to the server but I need to know when my sid is expired
    private fun sid(): String = runBlocking {
        async { authLoader.loadOnce().sid }.await()
    }

    @Serializable
    data class Auth(val status: String, val sid: String, val cid: Long)

    @Serializable
    data class Problem(val id: Int, val name: String, val code: String, val max_points: Double = 0.0)

    @Serializable
    data class Problems(val problems: List<Problem>)

    @Serializable
    data class Team(val id: Int, val account_id: Int, val login: String, val name: String, val role: String)

    @Serializable
    data class Users(val users: List<Team>)

    @Serializable
    data class Contest(
        val title: String,
        val start_date: String,
        val freeze_date: String,
        val finish_date: String,
        val rules: String
    )

    @Serializable
    sealed class Run

    @Serializable
    @SerialName("submit")
    data class Submit(
        val id: Int,
        val state_text: String,
        val problem_id: Int,
        val team_id: Int,
        val submit_time: String,
        val points: Double = 0.0
    ) : Run()

    @Serializable
    @SerialName("broadcast")
    @Suppress("unused")
    data class Broadcast(
        val text: String
    ) : Run()

    // NOTICE: May it
    @Serializable
    @SerialName("c.question")
    @Suppress("unused")
    data class Question(
        val text: String
    ) : Run()

    @Serializable
    @SerialName("contest")
    @Suppress("unused")
    data class ContestStart(
        val contest_start: Int
    ) : Run()

    private val authLoader =
        jsonLoaderService<Auth> { "$url/?f=login&login=$login&passwd=$password&json=1" }
    private val problemsLoader =
        jsonLoaderService<Problems> { "$url/problems?cid=${properties.getProperty("cid")}&sid=${sid()}&json=1" }
    private val usersLoader =
        jsonLoaderService<Users> { "$url/users?cid=${properties.getProperty("cid")}&sid=${sid()}&rows=1000&json=1" }
    private val contestLoader =
        jsonLoaderService<Contest> { "$url/contest_params?cid=${properties.getProperty("cid")}&sid=${sid()}&json=1" }
    private val runsLoader =
        jsonLoaderService<List<Run>> { "$url/console?cid=${properties.getProperty("cid")}&sid=${sid()}&rows=1000&json=1" }

    private fun getDateTime(dateTime: String, formatter: DateTimeFormatter) = LocalDateTime
        .parse(dateTime, formatter)
        .toKotlinLocalDateTime()
        .toInstant(TimeZone.of(timezone))

    override suspend fun loadOnce(): ContestParseResult {
        return parseAndUpdateStandings(
            problemsLoader.loadOnce(),
            usersLoader.loadOnce(),
            contestLoader.loadOnce(),
            runsLoader.loadOnce()
        )
    }

    private fun parseAndUpdateStandings(
        problems: Problems,
        users: Users,
        contest: Contest,
        runs: List<Run>
    ): ContestParseResult {
        val problemsList: List<ProblemInfo> = problems.problems
            .asSequence()
            .mapIndexed { index, problem ->
                ProblemInfo(
                    letter = problem.code,
                    name = problem.name,
                    color = Color(0x000000),
                    id = problem.id,
                    ordinal = index,
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) problem.max_points else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_PER_GROUP else null
                )
            }
            .toList()

        val teamList: List<TeamInfo> = users.users
            .asSequence()
            .filter { team -> team.role == "in_contest" }
            .map { team ->
                TeamInfo(
                    id = team.account_id,
                    name = team.name,
                    shortName = team.name,
                    contestSystemId = team.account_id.toString(),
                    groups = listOf(),
                    hashTag = null,
                    medias = mapOf()
                )
            }.toList()

        val runsFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'+1000'")
        val contestTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

        val startTime = getDateTime(contest.start_date, contestTimeFormatter)
        val contestLength = getDateTime(contest.finish_date, contestTimeFormatter) - startTime
        val freezeTime = getDateTime(contest.freeze_date, contestTimeFormatter) - startTime

        val contestInfo = ContestInfo(
            status = ContestStatus.OVER,
            resultType = resultType,
            startTime = startTime,
            contestLength = contestLength,
            freezeTime = freezeTime,
            problems = problemsList,
            teams = teamList
        )

        val resultRuns = runs
            .asSequence()
            .filterIsInstance(Submit::class.java)
            .map { it: Run -> it as Submit }
            .map { it: Submit ->
                logger.debug(
                    "SubmitTime: ${
                        getDateTime(
                            it.submit_time,
                            runsFormatter
                        )
                    }, time=${getDateTime(it.submit_time, runsFormatter) - startTime}"
                )
                RunInfo(
                    id = it.id,
                    if (contestInfo.resultType == ContestResultType.ICPC) ICPCRunResult(
                        isAccepted = ("OK" == it.state_text),
                        isAddingPenalty = ("OK" != it.state_text && "CE" != it.state_text),
                        isFirstToSolveRun = false,
                        result = it.state_text
                    ) else {
                        IOIRunResult(
                            score = it.points,
                            difference = 0.0,
                            scoreByGroup = listOf(it.points),
                            wrongVerdict = null
                        )
                    },
                    problemId = it.problem_id,
                    teamId = it.team_id,
                    percentage = if ("" == it.state_text) 0.0 else 1.0,
                    time = getDateTime(it.submit_time, runsFormatter) - startTime
                )
            }
            .toList()

        return ContestParseResult(
            contestInfo,
            resultRuns,
            emptyList()
        )
    }
}