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
import org.icpclive.api.ContestStatus
import org.icpclive.api.MediaType
import org.icpclive.api.RunInfo
import org.icpclive.cds.ProblemInfo
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.service.EmulationService
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.utils.humanReadable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.awt.Color
import java.util.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class PCMSEventsLoader {
    private fun loadProblemsInfo(problemsFile: String?): List<ProblemInfo> {
        val xml = Config.loadFile(problemsFile!!)
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


    suspend fun run() {
        coroutineScope {
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = 100000,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            launchICPCServices(contestData.problemsNumber, rawRunsFlow)

            val xmlLoader = object : RegularLoaderService<Document>() {
                override val url = properties.getProperty("url")
                override val login = properties.getProperty("login") ?: ""
                override val password = properties.getProperty("password") ?: ""
                override fun processLoaded(data: String) = Jsoup.parse(data, "", Parser.xmlParser())
            }

            val emulationSpeedProp: String? = properties.getProperty("emulation.speed")
            if (emulationSpeedProp == null) {
                val xmlLoaderFlow = MutableStateFlow(Document(""))
                launch(Dispatchers.IO) {
                    xmlLoader.run(xmlLoaderFlow, 5.seconds)
                }
                xmlLoaderFlow.collect {
                    parseAndUpdateStandings(it) { runBlocking { rawRunsFlow.emit(it) } }
                    DataBus.contestInfoUpdates.value = contestData.toApi()
                    if (contestData.status == ContestStatus.RUNNING) {
                        logger.info("Updated for contest time = ${contestData.contestTime}")
                    }
                }
            } else {
                val runs = mutableListOf<RunInfo>()
                parseAndUpdateStandings(xmlLoader.loadOnce()) { runs.add(it) }
                if (contestData.status != ContestStatus.OVER) {
                    throw IllegalStateException("Emulation mode require over contest")
                }
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                logger.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${emulationStartTime.humanReadable}")
                launch {
                    EmulationService(
                        emulationStartTime,
                        emulationSpeed,
                        runs.toList(),
                        contestData.toApi(),
                        rawRunsFlow
                    ).run()
                }
            }

        }
    }

    private fun parseAndUpdateStandings(element: Element, onRunChanges: (RunInfo) -> Unit) {
        if ("contest" == element.tagName()) {
            parseContestInfo(element, onRunChanges)
        } else {
            element.children().forEach { parseAndUpdateStandings(it, onRunChanges) }
        }
    }

    private var lastRunId = 0
    private fun parseContestInfo(element: Element, onRunChanges: (RunInfo) -> Unit) {
        val status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
        val contestTime = element.attr("time").toLong().milliseconds
        if (status == ContestStatus.RUNNING && contestData.status !== ContestStatus.RUNNING) {
            contestData.startTime = Clock.System.now() - contestTime
        }
        Config.advancedProperties.getProperty("contest.startTime")?.let {
            val unix = guessDatetimeFormat(it) ?: return@let
            contestData.startTime = unix
        }
        contestData.status = status
        contestData.contestTime = contestTime
        element.children().forEach { session: Element ->
            if ("session" == session.tagName()) {
                parseTeamInfo(contestData, session, onRunChanges)
            }
        }
    }

    private fun parseTeamInfo(contestInfo: PCMSContestInfo, element: Element, onRunChanges: (RunInfo) -> Unit) {
        val alias = element.attr("alias")
        contestInfo.getParticipant(alias)?.apply {
            for (i in element.children().indices) {
                runs[i] = parseProblemRuns(contestInfo, element.child(i), i, id, onRunChanges)
            }
        }
    }

    private fun parseProblemRuns(
        contestInfo: PCMSContestInfo,
        element: Element,
        problemId: Int,
        teamId: Int,
        onRunChanges: (RunInfo) -> Unit
    ): List<RunInfo> {
        if (contestInfo.status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children().mapIndexedNotNull { index, run ->
            parseRunInfo(contestInfo, run, problemId, teamId, index, onRunChanges)
        }
    }

    private fun parseRunInfo(
        contestInfo: PCMSContestInfo,
        element: Element,
        problemId: Int,
        teamId: Int,
        attemptId: Int,
        onRunChanges: (RunInfo) -> Unit
    ): RunInfo? {
        val time = element.attr("time").toLong().milliseconds
        if (time > contestInfo.contestTime) return null
        val isFrozen = time >= contestInfo.freezeTime
        val oldRun = contestInfo.teams[teamId].runs[problemId].getOrNull(attemptId)
        val percentage = when {
            isFrozen -> 0.0
            "undefined" == element.attr("outcome") -> 0.0
            else -> 1.0
        }
        val result = when {
            !(percentage >= 1.0) -> ""
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
            time = time.inWholeMilliseconds,
            isFirstSolvedRun = false
        )
        if (run != oldRun) {
            onRunChanges(run)
        }
        return run
    }

    private var contestData: PCMSContestInfo
    private val properties: Properties = Config.loadProperties("events")

    init {
        val problemInfo = loadProblemsInfo(properties.getProperty("problems.url"))
        val xml = Config.loadFile(properties.getProperty("teams.url"))
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val participants = doc.child(0)
        val teams = participants.children().withIndex().map { (index, participant) ->
            val participantName = participant.attr("name")
            val alias = participant.attr("id")
            val hallId = participant.attr("hall_id").takeIf { it.isNotEmpty() } ?: alias
            val shortName = participant.attr("shortname")
//                .split("(")[0]
//                .let { if (it.length >= 30) it.substring(0..27) + "..." else it }
                .takeIf { it.isNotEmpty() } ?: participantName
            val region = participant.attr("region").split(",")[0]
            val hashTag = participant.attr("hashtag")
            val groups = if (region.isEmpty()) emptySet() else mutableSetOf(region)
            val medias = listOfNotNull(
                participant.attr("screen").takeIf { it.isNotEmpty() }?.let { MediaType.SCREEN to it },
                participant.attr("camera").takeIf { it.isNotEmpty() }?.let { MediaType.CAMERA to it },
                participant.attr("record").takeIf { it.isNotEmpty() }?.let { MediaType.RECORD to it },
            ).associate { it }
            PCMSTeamInfo(
                index, alias, hallId, participantName, shortName,
                hashTag, groups, medias,
                problemInfo.size,
            )
        }
        contestData = PCMSContestInfo(problemInfo, teams, Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN)
        contestData.contestLength = properties.getProperty("contest.length")?.toInt()?.milliseconds ?: 5.hours
        contestData.freezeTime = properties.getProperty("freeze.time")?.toInt()?.milliseconds ?: 4.hours
        loadProblemsInfo(properties.getProperty("problems.url"))
    }

    companion object {
        private val logger = getLogger(PCMSEventsLoader::class)
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
