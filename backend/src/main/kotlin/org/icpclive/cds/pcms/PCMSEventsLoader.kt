package org.icpclive.cds.pcms

import kotlinx.coroutines.delay
import org.icpclive.Config.loadFile
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus.publishContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.NetworkUtils.openAuthorizedStream
import org.icpclive.cds.ProblemInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Collectors
import kotlin.random.Random
import kotlin.time.*
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

    private fun updateContest() {
        val url = properties.getProperty("url")
        val login = properties.getProperty("login")
        val password = properties.getProperty("password")
        val inputStream = openAuthorizedStream(url, login, password)
        val xml = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining())
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        parseAndUpdateStandings(doc)
    }

    override suspend fun run() {
        while (true) {
            try {
                updateContest()
                delay(5.seconds)
            } catch (e: IOException) {
                logger.error("error", e)
            } catch (e: InterruptedException) {
                logger.error("error", e)
            }
        }
    }

    private fun parseAndUpdateStandings(element: Element) {
        if ("contest" == element.tagName()) {
            val updatedContestInfo = parseContestInfo(element)
            publishContestInfo(updatedContestInfo)
            contestData = updatedContestInfo
        } else {
            element.children().forEach { parseAndUpdateStandings(it) }
        }
    }

    private var lastRunId = 0
    private fun parseContestInfo(element: Element): PCMSContestInfo {
        val updatedContestInfo = PCMSContestInfo(
            contestData.problems,
            contestData.teams.map { it.copy() },
            contestData.startTime,
            contestData.status
        )
        val previousStartTime = contestData.startTime
        val previousStatus = contestData.status
        if (emulationEnabled) {
            val timeByEmulation = ((System.currentTimeMillis() - emulationStartTime) * emulationSpeed).toLong()
            if (timeByEmulation < 0) {
                updatedContestInfo.contestTime = 0
                updatedContestInfo.status = ContestStatus.BEFORE
            } else if (timeByEmulation < updatedContestInfo.contestLength) {
                updatedContestInfo.contestTime = timeByEmulation
                updatedContestInfo.status = ContestStatus.RUNNING
            } else {
                updatedContestInfo.contestTime = updatedContestInfo.contestLength.toLong()
                updatedContestInfo.status = ContestStatus.OVER
            }
            updatedContestInfo.startTime = emulationStartTime
        } else {
            updatedContestInfo.status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
            updatedContestInfo.contestTime = element.attr("time").toLong()
            when (updatedContestInfo.status) {
                ContestStatus.BEFORE, ContestStatus.UNKNOWN, ContestStatus.OVER -> {}
                ContestStatus.RUNNING -> if (previousStatus !== ContestStatus.RUNNING || previousStartTime == 0L) {
                    updatedContestInfo.startTime = System.currentTimeMillis() - updatedContestInfo.contestTime
                } else {
                    updatedContestInfo.startTime = previousStartTime
                }
            }
        }
        element.children().forEach { session: Element ->
            if ("session" == session.tagName()) {
                parseTeamInfo(updatedContestInfo, contestData, session, updatedContestInfo.contestTime)
            }
        }
        updatedContestInfo.calculateRanks()
        updatedContestInfo.makeRuns()
        return updatedContestInfo
    }

    private fun parseTeamInfo(contestInfo: PCMSContestInfo, oldContestInfo: PCMSContestInfo, element: Element, timeBound: Long) {
        val alias = element.attr("alias")
        val oldParticipant = oldContestInfo.getParticipant(alias)
        contestInfo.getParticipant(alias)?.apply {
            solvedProblemsNumber = element.attr("solved").toInt()
            penalty = element.attr("penalty").toInt()
            for (i in element.children().indices) {
                runs[i] = parseProblemRuns(contestInfo, element.child(i), oldParticipant?.runs?.get(i) ?: emptyList(), i, id, timeBound)
            }
        }
    }

    private fun parseProblemRuns(contestInfo: PCMSContestInfo, element: Element, oldRuns: List<PCMSRunInfo>, problemId: Int, teamId: Int, timeBound: Long): List<PCMSRunInfo> {
        if (contestInfo.status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children().mapIndexedNotNull { index, run ->
            parseRunInfo(contestInfo, run, oldRuns.getOrNull(index), problemId, teamId, timeBound)
        }
    }

    private fun parseRunInfo(contestInfo: PCMSContestInfo, element: Element, oldRun: PCMSRunInfo?, problemId: Int, teamId: Int, timeBound: Long): PCMSRunInfo? {
        val time = element.attr("time").toLong()
        if (time > timeBound) return null
        val isFrozen = time >= contestInfo.freezeTime
        val percentage = when {
            isFrozen -> 0.0
            emulationEnabled -> if (timeBound - time >= 60000) 1.0 else minOf(1.0, (oldRun?.percentage ?: 0.0) + Random.nextDouble(1.0))
            "undefined" == element.attr("outcome") -> 0.0
            else -> 1.0
        }
        val isJudged = percentage >= 1.0
        val result = when {
            !isJudged -> ""
            "yes" == element.attr("accepted") -> "AC"
            else -> outcomeMap.getOrDefault(element.attr("outcome"), "WA")
        }
        return PCMSRunInfo(
            oldRun?.id ?: lastRunId++,
            isJudged, result, problemId, time, teamId,
            percentage,
            if (isJudged == oldRun?.isJudged && result == oldRun.result)
                oldRun.lastUpdateTime
            else
                contestInfo.contestTime
        )
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
            emulationStartTime = properties.getProperty("emulation.startTime").toLong() * 1000
            logger.info("Running in emulation mode with speed x${emulationSpeed} and startTime = ${Date(emulationStartTime)}")
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
        contestData = PCMSContestInfo(problemInfo, teams, 0, ContestStatus.UNKNOWN)
        contestData.contestLength = properties.getProperty("contest.length", "" + 5 * 60 * 60 * 1000).toInt()
        contestData.freezeTime = properties.getProperty("freeze.time", "" + 4 * 60 * 60 * 1000).toInt()
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