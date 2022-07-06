package org.icpclive.cds.ejudge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.config.Config
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Mike Perveev
 */
class EjudgeDataSource : ContestDataSource {
    override suspend fun run() {
        coroutineScope {
            val xmlLoaderFlow = MutableStateFlow(Document(""))
            launch(Dispatchers.IO) {
                xmlLoader.run(xmlLoaderFlow, 5.seconds)
            }
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            val contestInfoFlow = MutableStateFlow(contestData.toApi())
            launchICPCServices(rawRunsFlow, contestInfoFlow)
            xmlLoaderFlow.collect {
                if (it.children().size != 0) {
                    parseContestInfo(it.children()[0]) { runBlocking { rawRunsFlow.emit(it) } }
                    contestInfoFlow.value = contestData.toApi()
                    if (contestData.status == ContestStatus.RUNNING) {
                        logger.info("Updated for contest time = ${contestData.contestTime}")
                    }
                }
            }
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        val allRuns = mutableListOf<RunInfo>()
        val element = xmlLoader.loadOnce()
        parseContestInfo(element.children()[0]) { allRuns.add(it) }
        if (contestData.status != ContestStatus.OVER) {
            throw IllegalStateException("Emulation mode require over contest")
        }
        return ContestParseResult(contestData.toApi(), allRuns)
    }

    private fun parseProblemsInfo(doc: Document): List<ProblemInfo> {
        val config = doc.child(0)
        config.children().forEach {
            if ("problems" == it.tagName()) {
                return it.children().map { element ->
                    ProblemInfo(
                        element.attr("short_name"),
                        element.attr("short_name"),
                        null
                    )
                }
            }
        }

        logger.error("There is no <problems> tag in external XML log")
        return emptyList()
    }

    private fun parseTeamsInfo(doc: Document, problemsNumber: Int): List<EjudgeTeamInfo> {
        val config = doc.child(0)
        config.children().forEach {
            if ("users" == it.tagName()) {
                return it.children().withIndex().map { (index, participant) ->
                    val participantName = participant.attr("name")
                    val alias = participant.attr("id")
                    val groups = listOf<String>()
                    val medias = mutableMapOf<MediaType, String>()
                    EjudgeTeamInfo(
                        TeamInfo(
                            index,
                            participantName,
                            participantName,
                            alias,
                            groups,
                            participantName,
                            medias
                        ),
                        problemsNumber
                    )
                }
            }
        }

        logger.error("There is no <users> tag in external XML log")
        return emptyList()
    }

    private fun parseContestTime(doc: Document): Pair<Duration, Duration> {
        val config = doc.child(0)
        val duration = config.attr("duration")
        val fogTime = config.attr("fog_time")
        return Pair(duration.toLong().seconds, (duration.toLong() - fogTime.toLong()).seconds)
    }

    private fun parseEjudgeTime(time: String): Instant {
        val formattedTime = time
            .replace("/", "-")
            .replace(" ", "T")
        return guessDatetimeFormat(formattedTime)
    }

    private fun parseContestInfo(element: Element, onRunChanges: (RunInfo) -> Unit) {
        val dur = element.attr("duration").toLong()
        val startTime = parseEjudgeTime(element.attr("start_time"))
        val endTime = startTime.plus(dur.seconds)
        val currentTime = parseEjudgeTime(element.attr("current_time"))

        val status: ContestStatus = if (currentTime >= endTime) {
            ContestStatus.OVER
        } else if (currentTime < startTime) {
            ContestStatus.BEFORE
        } else {
            ContestStatus.RUNNING
        }

        if (status == ContestStatus.RUNNING && contestData.status !== ContestStatus.RUNNING) {
            contestData.startTime = startTime
        }
        contestData.status = status
        contestData.contestTime = currentTime - startTime

        element.children().forEach {
            if ("runs" == it.tagName()) {
                parseRuns(contestData, it, onRunChanges)
            }
        }
    }

    private fun parseRuns(
        contestInfo: EjudgeContestInfo,
        element: Element,
        onRunChanges: (RunInfo) -> Unit
    ) {
        if (contestInfo.status == ContestStatus.BEFORE) {
            return
        }
        element.children().forEach { run ->
            parseRunInfo(contestInfo, run, onRunChanges)
        }
    }

    private fun parseRunInfo(
        contestInfo: EjudgeContestInfo,
        element: Element,
        onRunChanges: (RunInfo) -> Unit
    ) {
        val time = element.attr("time").toLong().seconds
        if (time > contestInfo.contestTime) {
            return
        }

        val teamSystemId = element.attr("user_id").toInt()
        val teamId = contestInfo.teams[teamSystemId.toString()]!!.teamInfo.id
        val runId = element.attr("run_id").toInt()

        // Ejudge has 1-indexed problem numeration
        val problemId = element.attr("prob_id").toInt() - 1
        val isFrozen = time >= contestInfo.freezeTime
        val oldRun = contestInfo.teams[teamSystemId.toString()]!!.runs[problemId].getOrDefault(runId, null)
        val result = when {
            isFrozen -> ""
            else -> statusMap.getOrDefault(element.attr("status"), "WA")
        }
        val percentage = when {
            isFrozen -> 0.0
            "" == result -> 0.0
            else -> 1.0
        }

        val run = RunInfo(
            id = oldRun?.id ?: element.attr("run_id").toInt(),
            isAccepted = "AC" == result,
            isJudged = "" != result,
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
        contestInfo.teams[teamSystemId.toString()]!!.runs[problemId][runId] = run
    }

    private var contestData: EjudgeContestInfo
    private var contestInfoFlow: MutableStateFlow<ContestInfo>
    private val properties: Properties = Config.loadProperties("events")
    private val xmlLoader = object : RegularLoaderService<Document>(null) {
        override val url = properties.getProperty("url")
        override fun processLoaded(data: String) = Jsoup.parse(data, "", Parser.xmlParser())
    }

    init {
        val doc: Document
        runBlocking {
            doc = xmlLoader.loadOnce()
        }

        val problemsInfo = parseProblemsInfo(doc)
        val teamsInfo = parseTeamsInfo(doc, problemsInfo.size)
        val timeInfo = parseContestTime(doc)

        contestData = EjudgeContestInfo(
            problemsInfo,
            teamsInfo.associateBy { it.teamInfo.contestSystemId },
            Instant.fromEpochMilliseconds(0),
            ContestStatus.UNKNOWN,
            timeInfo.first,
            timeInfo.second
        )
        contestInfoFlow = MutableStateFlow(contestData.toApi())
    }

    companion object {
        private val logger = getLogger(EjudgeDataSource::class)
        private val statusMap = mapOf(
            "OK" to "AC",
            "CE" to "CE",
            "RT" to "RE",
            "TL" to "TL",
            "PE" to "PE",
            "WA" to "WA",
            "CF" to "FL",
            "PT" to "",
            "AC" to "OK",
            "IG" to "",
            "DQ" to "",
            "PD" to "",
            "ML" to "ML",
            "SE" to "SV",
            "SV" to "",
            "WT" to "IL",
            "PR" to "",
            "RJ" to "",
            "SK" to "",
            "SY" to "",
            "SM" to "",
            "RU" to "",
            "CD" to "",
            "CG" to "",
            "AV" to "",
            "EM" to "",
            "VS" to "",
            "VT" to "",
        )
    }
}
