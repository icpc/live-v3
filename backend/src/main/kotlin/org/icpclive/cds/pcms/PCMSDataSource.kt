package org.icpclive.cds.pcms

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.RunsBufferService
import org.icpclive.service.XmlLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PCMSDataSource(val properties: Properties) : ContestDataSource {
    private fun getLoader(): RegularLoaderService<Document> {
        val auth = run {
            val login = properties.getCredentials("login")
            val password = properties.getCredentials("password")
            login?.let { ClientAuth.Basic(login, password!!) }
        }
        return object : XmlLoaderService(auth) {
            override val url = properties.getProperty("url")
        }
    }

    val runIds = mutableMapOf<String, Int>()
    val teamIds = mutableMapOf<String, Int>()
    var startTime = Instant.fromEpochMilliseconds(0)

    val freezeTime = properties.getProperty("freeze.time")?.toInt()?.milliseconds ?: 4.hours


    override suspend fun run() {
        coroutineScope {
            val xmlLoader = getLoader()
            val runsBufferFlow = MutableStateFlow<List<RunInfo>>(emptyList())
            val rawRunsFlow = reliableSharedFlow<RunInfo>()
            launch { RunsBufferService(runsBufferFlow, rawRunsFlow).run() }
            val contestInfoFlow = MutableStateFlow(ContestInfo.unknown())
            launchICPCServices(rawRunsFlow, contestInfoFlow)
            xmlLoader.run(5.seconds).collect {
                val (info, runs) = parseAndUpdateStandings(it.documentElement)
                contestInfoFlow.value = info
                runsBufferFlow.value = runs
            }
        }
    }

    override suspend fun loadOnce() = parseAndUpdateStandings(getLoader().loadOnce().documentElement).also {
        require(it.contestInfo.status == ContestStatus.OVER) {
            "Emulation mode require over contest"
        }
    }

    private fun parseAndUpdateStandings(element: Element) = parseContestInfo(element.child("contest"))

    private fun parseContestInfo(element: Element) : ContestParseResult {
        val status = ContestStatus.valueOf(element.getAttribute("status").uppercase(Locale.getDefault()))
        val contestTime = element.getAttribute("time").toLong().milliseconds
        val contestLength = element.getAttribute("length").toInt().milliseconds
        if (status == ContestStatus.RUNNING && startTime.epochSeconds == 0L) {
            startTime = Clock.System.now() - contestTime
        }
        val problems = element
            .child("challenge")
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
                startTime,
                contestLength,
                freezeTime,
                problems,
                teamsAndRuns.map { it.first }.sortedBy { it.id },
            ),
            teamsAndRuns.flatMap { it.second }
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
        val result = when {
            percentage < 1.0 -> ""
            "yes" == element.getAttribute("accepted") -> "AC"
            else -> outcomeMap.getOrDefault(element.getAttribute("outcome"), "WA")
        }
        return RunInfo(
            id = id,
            isAccepted = "AC" == result,
            isJudged = percentage >= 1.0,
            isAddingPenalty = "AC" != result && "CE" != result,
            result = result,
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
