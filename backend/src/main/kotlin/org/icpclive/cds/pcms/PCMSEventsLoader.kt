package org.icpclive.cds.pcms

import guessDatetimeFormat
import humanReadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.*
import org.icpclive.Config.loadFile
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.api.Scoreboard
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.OptimismLevel
import org.icpclive.cds.ProblemInfo
import org.icpclive.service.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.awt.Color
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PCMSEventsLoader : EventsLoader() {
    private fun loadProblemsInfo(problemsFile: String?) : List<ProblemInfo> {
        val xml = loadFile(problemsFile!!)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val problems = doc.child(0)
        return problems.children().map { element ->
            ProblemInfo(
                element.attr("alias"),
                element.attr("name"),
                if (element.attr("color").isEmpty()) Color.BLACK else Color.decode(element.attr("color"))
            )
        }
    }

    private val rawRunsFlow = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    override suspend fun run() {
        coroutineScope {
            val xmlLoaderFlow = MutableStateFlow(Document(""))
            launch { FirstToSolveService(contestData.problemsNumber, rawRunsFlow, DataBus.runsUpdates).run() }
            launch(Dispatchers.IO) {
                object : RegularLoaderService<Document>(
                    xmlLoaderFlow,
                    5.seconds / emulationSpeed
                ) {
                    override val url = properties.getProperty("url")
                    override val login = properties.getProperty("login")
                    override val password = properties.getProperty("password")
                    override fun processLoaded(data: String) = Jsoup.parse(data, "", Parser.xmlParser())
                }.run()
            }
            launch { ICPCNormalScoreboardService(contestData.problemsNumber, DataBus.runsUpdates, DataBus.scoreboardFlow).run() }
            launch { ICPCOptimisticScoreboardService(contestData.problemsNumber, DataBus.runsUpdates, DataBus.optimisticScoreboardFlow).run() }
            launch { ICPCPessimisticScoreboardService(contestData.problemsNumber, DataBus.runsUpdates, DataBus.pessimisticScoreboardFlow).run() }

            xmlLoaderFlow.collect {
                parseAndUpdateStandings(it)
                DataBus.contestInfoFlow.value = contestData.toApi()
                if (contestData.status == ContestStatus.RUNNING) {
                    logger.info("Updated for contest time = ${contestData.contestTime}")
                }
            }
        }
    }

    private fun parseAndUpdateStandings(element: Element) {
        if ("contest" == element.tagName()) {
            parseContestInfo(element)
        } else {
            element.children().forEach { parseAndUpdateStandings(it) }
        }
    }

    private var lastRunId = 0
    private fun parseContestInfo(element: Element) {
        if (emulationEnabled) {
            val timeByEmulation = (Clock.System.now() - emulationStartTime) * emulationSpeed
            if (timeByEmulation.isNegative()) {
                contestData.contestTime = 0.milliseconds
                contestData.status = ContestStatus.BEFORE
            } else if (timeByEmulation < contestData.contestLength) {
                contestData.contestTime = timeByEmulation
                contestData.status = ContestStatus.RUNNING
            } else {
                contestData.contestTime = contestData.contestLength
                contestData.status = ContestStatus.OVER
            }
            contestData.startTime = emulationStartTime
        } else {
            val status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
            val contestTime = element.attr("time").toLong().milliseconds
            if (status == ContestStatus.RUNNING && contestData.status !== ContestStatus.RUNNING) {
                contestData.startTime = Clock.System.now() - contestTime
            }
            contestData.status = status
            contestData.contestTime = contestTime
        }
        element.children().forEach { session: Element ->
            if ("session" == session.tagName()) {
                parseTeamInfo(contestData, session)
            }
        }
        contestData.calculateRanks()
    }

    private fun parseTeamInfo(contestInfo: PCMSContestInfo, element: Element) {
        val alias = element.attr("alias")
        contestInfo.getParticipant(alias)?.apply {
            solvedProblemsNumber = element.attr("solved").toInt()
            penalty = element.attr("penalty").toInt()
            for (i in element.children().indices) {
                runs[i] = parseProblemRuns(contestInfo, element.child(i), i, id)
            }
        }
    }

    private fun parseProblemRuns(contestInfo: PCMSContestInfo, element: Element, problemId: Int, teamId: Int): List<PCMSRunInfo> {
        if (contestInfo.status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children().mapIndexedNotNull { index, run ->
            parseRunInfo(contestInfo, run, problemId, teamId, index)
        }
    }

    private fun parseRunInfo(contestInfo: PCMSContestInfo, element: Element, problemId: Int, teamId: Int, attemptId: Int): PCMSRunInfo? {
        val time = element.attr("time").toLong().milliseconds
        if (time > contestInfo.contestTime) return null
        val isFrozen = time >= contestInfo.freezeTime
        val oldRun = contestInfo.teams[teamId].runs[problemId].getOrNull(attemptId)
        val percentage = when {
            isFrozen -> 0.0
            emulationEnabled -> if (contestInfo.contestTime - time >= 60.seconds) 1.0 else minOf(1.0, (oldRun?.percentage ?: 0.0) + Random.nextDouble(1.0))
            "undefined" == element.attr("outcome") -> 0.0
            else -> 1.0
        }
        val isJudged = percentage >= 1.0
        val result = when {
            !isJudged -> ""
            "yes" == element.attr("accepted") -> "AC"
            else -> outcomeMap.getOrDefault(element.attr("outcome"), "WA")
        }
        val run = PCMSRunInfo(
            oldRun?.id ?: lastRunId++,
            isJudged, result, problemId, time.inWholeMilliseconds, teamId,
            percentage,
            if (isJudged == oldRun?.isJudged && result == oldRun.result)
                oldRun.lastUpdateTime
            else
                contestInfo.contestTime.inWholeMilliseconds
        )
        if (run.toApi() != oldRun?.toApi()) {
            runBlocking { rawRunsFlow.emit(run.toApi()) }
        }
        return run
    }

    private var contestData: PCMSContestInfo
    private val properties: Properties = loadProperties("events")

    init {
        val emulationSpeedProp : String? = properties.getProperty("emulation.speed")
        if (emulationSpeedProp == null) {
            emulationEnabled = false
            emulationSpeed = 1.0
        } else {
            emulationEnabled = true
            emulationSpeed = emulationSpeedProp.toDouble()
            emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
            logger.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${emulationStartTime.humanReadable}")
        }
        val problemInfo = loadProblemsInfo(properties.getProperty("problems.url"))
        val fn = properties.getProperty("teams.url")
        val xml = loadFile(fn)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val participants = doc.child(0)
        val teams = participants.children().withIndex().map { (index, participant) ->
            val participantName = participant.attr("name")
            val alias = participant.attr("id")
            val hallId = participant.attr("hall_id").takeIf { it.isNotEmpty() } ?: alias
            val shortName = participant.attr("shortname")
                .split("(")[0]
                .let { if (it.length >= 30) it.substring(0..27) + "..." else it }
            val region = participant.attr("region").split(",")[0]
            val hashTag = participant.attr("hashtag")
            val groups = if (region.isEmpty()) emptySet() else mutableSetOf(region)
            PCMSTeamInfo(
                index, alias, hallId, participantName, shortName,
                hashTag, groups, problemInfo.size
            )
        }
        contestData = PCMSContestInfo(problemInfo, teams, Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN)
        contestData.contestLength = properties.getProperty("contest.length", "" + 5 * 60 * 60 * 1000).toInt().milliseconds
        contestData.freezeTime = properties.getProperty("freeze.time", "" + 4 * 60 * 60 * 1000).toInt().milliseconds
        contestData.groups.addAll(teams.flatMap { it.groups }.filter { it.isNotEmpty() })
        loadProblemsInfo(properties.getProperty("problems.url"))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PCMSEventsLoader::class.java)
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