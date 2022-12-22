package org.icpclive.cds.pcms

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.common.xmlLoaderService
import org.icpclive.util.child
import org.icpclive.util.children
import org.icpclive.util.getCredentials
import org.icpclive.util.getLogger
import org.w3c.dom.Element
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PCMSDataSource(val properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val login = properties.getCredentials("login", creds)
    private val password = properties.getCredentials("password", creds)
    private val dataLoader = xmlLoaderService(login?.let { ClientAuth.Basic(login, password!!) }) {
        properties.getProperty("url")
    }

    private val resultType = ContestResultType.valueOf(
        properties.getProperty("standings.resultType")?.toString()?.uppercase() ?: "ICPC"
    )
    private val minScore: Float = properties.getProperty("standings.minScore", "0.0").toFloat()
    private val maxScore: Float = properties.getProperty("standings.maxScore", "100.0").toFloat()
    val runIds = mutableMapOf<String, Int>()
    val teamIds = mutableMapOf<String, Int>()
    var startTime = Instant.fromEpochMilliseconds(0)

    val freezeTime = properties.getProperty("freeze.time")?.toInt()?.milliseconds ?: 4.hours

    override suspend fun loadOnce() = parseAndUpdateStandings(dataLoader.loadOnce().documentElement)
    private fun parseAndUpdateStandings(element: Element) = parseContestInfo(element.child("contest"))

    private fun loadCustomProblems() : Element {
        val problemsUrl = properties.getProperty("problems.url")
        val problemsLoader = xmlLoaderService { problemsUrl }

        // TODO: Async loading
        return runBlocking {
            problemsLoader.loadOnce().documentElement
        }
    }

    private fun parseContestInfo(element: Element) : ContestParseResult {
        val status = ContestStatus.valueOf(element.getAttribute("status").uppercase(Locale.getDefault()))
        val contestTime = element.getAttribute("time").toLong().milliseconds
        val contestLength = element.getAttribute("length").toInt().milliseconds
        if (status == ContestStatus.RUNNING && startTime.epochSeconds == 0L) {
            startTime = Clock.System.now() - contestTime
        }

        var problemsElement = element.child("challenge")

        if(properties.containsKey("problems.url")) {
            problemsElement = loadCustomProblems()
        }

        val problems = problemsElement
            .children("problem")
            .mapIndexed { index, it ->
                ProblemInfo(
                    it.getAttribute("alias"),
                    it.getAttribute("name"),
                    it.getAttribute("color").takeIf { it.isNotEmpty() },
                    index,
                    index
                )
            }.toList()

        val teamsAndRuns = element
            .children("session")
            .map { parseTeamInfo(it, problems, contestTime) }
            .toList()
        if (status == ContestStatus.RUNNING) {
            logger.info("Loaded contestInfo for time = $contestTime")
        }
        return ContestParseResult(
            ContestInfo(
                status,
                resultType,
                startTime,
                contestLength,
                freezeTime,
                problems,
                teamsAndRuns.map { it.first }.sortedBy { it.id },
                minScore = minScore,
                maxScore = maxScore
            ),
            teamsAndRuns.flatMap { it.second },
            emptyList()
        )
    }

    private fun parseTeamInfo(element: Element, problems:List<ProblemInfo>, contestTime: Duration) : Pair<TeamInfo, List<RunInfo>> {
        fun String.attr() = element.getAttribute(this).takeIf { it.isNotEmpty() }
        val alias = "alias".attr()!!
        val team = TeamInfo(
            id = teamIds.getOrPut(alias) { teamIds.size },
            name = "party".attr()!!,
            shortName = "shortname".attr() ?: "party".attr()!!,
            hashTag = "hashtag".attr(),
            groups = "region".attr()?.split(",") ?: emptyList(),
            medias = listOfNotNull(
                "screen".attr()?.let { TeamMediaType.SCREEN to MediaType.Video(it) },
                "camera".attr()?.let { TeamMediaType.CAMERA to MediaType.Video(it) },
                "record".attr()?.let { TeamMediaType.RECORD to MediaType.Video(it) },
            ).associate { it },
            contestSystemId = alias,
        )
        val runs =
            element.children("problem").flatMap { problem ->
                parseProblemRuns(
                    problem,
                    team.id,
                    problems.single { it.letter == problem.getAttribute("alias") }.id,
                    contestTime
                )
            }.toList()
        return team to runs
    }

    private fun parseProblemRuns(
        element: Element,
        teamId: Int,
        problemId: Int,
        contestTime: Duration,
    ): Sequence<RunInfo> {
        return element.children()
            .filter { it.getAttribute("time").toLong().milliseconds <= contestTime }
            .map { parseRunInfo(it, teamId, problemId) }
    }

    private fun parseRunInfo(
        element: Element,
        teamId: Int,
        problemId: Int,
    ): RunInfo {
        val time = element.getAttribute("time").toLong().milliseconds
        val isFrozen = time >= freezeTime
        val id = runIds.getOrPut(element.getAttribute("run-id")) { runIds.size }
        val percentage = when {
            isFrozen -> 0.0
            "undefined" == element.getAttribute("outcome") -> 0.0
            else -> 1.0
        }

        return RunInfo(
            id = id,
            when (resultType) {
                ContestResultType.IOI -> {
                    val score = element.getAttribute("score").toDouble()
                    IOIRunResult(
                        score = score,
                        difference = 0.0,
                        scoreByGroup = listOf(score)
                    )
                }
                ContestResultType.ICPC -> {
                    val result = when {
                        percentage < 1.0 -> null
                        "yes" == element.getAttribute("accepted") -> "AC"
                        else -> outcomeMap.getOrDefault(element.getAttribute("outcome"), "WA")
                    }
                    result?.let {
                        ICPCRunResult(
                            isAccepted = "AC" == it,
                            isAddingPenalty = "AC" != it && "CE" != it,
                            result = it,
                            isFirstToSolveRun = false
                        )
                    }
                }
            },
            problemId = problemId,
            teamId = teamId,
            percentage = percentage,
            time = time,
        )
    }

    companion object {
        private val logger = getLogger(PCMSDataSource::class)
        private val outcomeMap = mapOf(
            "undefined" to "UD",
            "fail" to "FL",
            "unknown" to "",
            "accepted" to "AC",
            "compilation-error" to "CE",
            "wrong-answer" to "WA",
            "presentation-error" to "PE",
            "runtime-error" to "RE",
            "time-limit-exceeded" to "TL",
            "memory-limit-exceeded" to "ML",
            "output-limit-exceeded" to "OL",
            "idleness-limit-exceeded" to "IL",
            "security-violation" to "SV",
        )
    }
}
