package org.icpclive.cds.pcms

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.BasicAuth
import org.icpclive.utils.getLogger
import org.icpclive.utils.processCreds
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PCMSDataSource(val properties: Properties) : ContestDataSource {
    private fun getLoader(): RegularLoaderService<Document> {
        val auth = run {
            val login = properties.getProperty("login")?.processCreds()
            val password = properties.getProperty("password")?.processCreds()
            if (login != null) {
                BasicAuth(login, password!!)
            } else {
                null
            }
        }
        return object : RegularLoaderService<Document>(auth) {
            override val url = properties.getProperty("url")
            override fun processLoaded(data: String) = Jsoup.parse(data, "", Parser.xmlParser())
        }
    }

    private var lastRunId = 0
    val runs = mutableMapOf<String, RunInfo>()
    var problems = emptyList<ProblemInfo>();
    val teams = mutableMapOf<String, TeamInfo>()
    var startTime = Instant.fromEpochMilliseconds(0)
    var status = ContestStatus.UNKNOWN
    val contestLength = properties.getProperty("contest.length")?.toInt()?.milliseconds ?: 5.hours
    val freezeTime = properties.getProperty("freeze.time")?.toInt()?.milliseconds ?: 4.hours

    private val contestInfo
        get() = ContestInfo(
            status,
            startTime,
            contestLength,
            freezeTime,
            problems,
            teams.values.sortedBy { it.id },
        )


    override suspend fun run() {
        coroutineScope {
            val xmlLoader = getLoader()
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            val contestInfoFlow = MutableStateFlow(contestInfo)
            launchICPCServices(rawRunsFlow, contestInfoFlow)
            xmlLoader.run(5.seconds).collect {
                parseAndUpdateStandings(it) { runBlocking { rawRunsFlow.emit(it) } }
                contestInfoFlow.value = contestInfo
            }
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        val xmlLoader = getLoader()
        val runs = mutableListOf<RunInfo>()
        parseAndUpdateStandings(xmlLoader.loadOnce()) { runs.add(it) }
        if (status != ContestStatus.OVER) {
            throw IllegalStateException("Emulation mode require over contest")
        }
        return ContestParseResult(contestInfo, runs.toList())
    }

    private fun parseAndUpdateStandings(element: Element, onRunChanges: (RunInfo) -> Unit) {
        if ("contest" == element.tagName()) {
            parseContestInfo(element, onRunChanges)
        } else {
            element.children().forEach { parseAndUpdateStandings(it, onRunChanges) }
        }
    }

    private fun parseContestInfo(element: Element, onRunChanges: (RunInfo) -> Unit) {
        val status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
        val contestTime = element.attr("time").toLong().milliseconds
        if (status == ContestStatus.RUNNING && this.status !== ContestStatus.RUNNING) {
            startTime = Clock.System.now() - contestTime
        }
        this.status = status
        problems = element
            .children()
            .single { it.tagName() == "challenge" }
            .children()
            .filter { it: Element -> it.tagName() == "problem" }
            .map {
                ProblemInfo(
                    it.attr("alias"),
                    it.attr("name"),
                    it.attr("color").takeIf { it.isNotEmpty() }
                )
            }
        element
            .children()
            .asSequence()
            .filter { it.tagName() == "session" }
            .forEach { parseTeamInfo(it, contestTime, onRunChanges) }
        if (status == ContestStatus.RUNNING) {
            logger.info("Loaded contestInfo for time = $contestTime")
        }
    }

    private fun parseTeamInfo(element: Element, contestTime: Duration, onRunChanges: (RunInfo) -> Unit) {
        fun String.attr() = element.attr(this).takeIf { it.isNotEmpty() }
        val alias = "alias".attr()!!
        val teamId = teams[alias]?.id ?: teams.size
        val team = TeamInfo(
            id = teamId,
            name = "party".attr()!!,
            shortName = "shortname".attr() ?: "party".attr()!!,
            hashTag = "hashtag".attr(),
            groups = "region".attr()?.split(",") ?: emptyList(),
            medias = listOfNotNull(
                "screen".attr()?.let { MediaType.SCREEN to it },
                "camera".attr()?.let { MediaType.CAMERA to it },
                "record".attr()?.let { MediaType.RECORD to it },
            ).associate { it },
            contestSystemId = alias,
        )
        teams[team.contestSystemId] = team
        for (problem in element.children()) {
            if (problem.tagName() != "problem") continue
            val index = problems.indexOfFirst { it.letter == problem.attr("alias") }
            require(index != -1)
            parseProblemRuns(problem, team.id, index, contestTime, onRunChanges)
        }
    }

    private fun parseProblemRuns(
        element: Element,
        teamId: Int,
        problemId: Int,
        contestTime: Duration,
        onRunChanges: (RunInfo) -> Unit
    ): List<RunInfo> {
        if (status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children()
            .asSequence()
            .filter { it.attr("time").toLong().milliseconds <= contestTime }
            .map { parseRunInfo(it, teamId, problemId, onRunChanges) }
            .toList()
    }

    private fun parseRunInfo(
        element: Element,
        teamId: Int,
        problemId: Int,
        onRunChanges: (RunInfo) -> Unit
    ): RunInfo {
        val time = element.attr("time").toLong().milliseconds
        val isFrozen = time >= freezeTime
        val oldRun = runs[element.attr("run-id")]
        val percentage = when {
            isFrozen -> 0.0
            "undefined" == element.attr("outcome") -> 0.0
            else -> 1.0
        }
        val result = when {
            percentage < 1.0 -> ""
            "yes" == element.attr("accepted") -> "AC"
            else -> outcomeMap.getOrDefault(element.attr("outcome"), "WA")
        }
        val run = RunInfo(
            id = oldRun?.id ?: lastRunId++,
            isAccepted = "AC" == result,
            isJudged = percentage >= 1.0,
            isAddingPenalty = "AC" != result && "CE" != result,
            result = result,
            problemId = problemId,
            teamId = teamId,
            percentage = percentage,
            time = time,
            isFirstSolvedRun = false
        )
        if (run != oldRun) {
            onRunChanges(run)
        }
        return run
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
