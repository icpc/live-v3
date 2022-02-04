package org.icpclive.cds.pcms

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

class PCMSEventsLoader : EventsLoader() {
    @Throws(IOException::class)
    fun loadProblemsInfo(problemsFile: String?) : List<ProblemInfo> {
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

    @Throws(IOException::class)
    private fun updateContest() {
        try {
            val url = properties.getProperty("url")
            val login = properties.getProperty("login")
            val password = properties.getProperty("password")
            val inputStream = openAuthorizedStream(url, login, password)
            val xml = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining())
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())
            parseAndUpdateStandings(doc)
        } catch (e: IOException) {
            logger.error("error", e)
        }
    }

    override fun run() {
        //log.debug(check.getName() + " " + check.getShortName());
        while (true) {
            try {
                while (true) {
                    updateContest()
                    Thread.sleep(5000)
                }
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
        val updatedContestInfo = PCMSContestInfo(contestData.problems, contestData.teams.map { it.copy() })
        val previousStartTime = contestData.startTime
        val previousStatus = contestData.status
        updatedContestInfo.status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
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
                parseTeamInfo(updatedContestInfo, session, updatedContestInfo.contestTime)
            }
        }
        updatedContestInfo.calculateRanks()
        updatedContestInfo.makeRuns()
        return updatedContestInfo
    }

    private fun parseTeamInfo(contestInfo: PCMSContestInfo, element: Element, timeBound: Long) {
        val alias = element.attr("alias")
        contestInfo.getParticipant(alias)?.apply {
            solvedProblemsNumber = element.attr("solved").toInt()
            penalty = element.attr("penalty").toInt()
            for (i in element.children().indices) {
                runs[i] = parseProblemRuns(contestInfo, element.child(i), runs[i], i, id, timeBound)
            }
        }
    }

    private fun parseProblemRuns(contestInfo: PCMSContestInfo, element: Element, oldRuns: List<PCMSRunInfo>, problemId: Int, teamId: Int, timeBound: Long): List<PCMSRunInfo> {
        if (contestInfo.status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children().mapIndexed { index, run ->
            parseRunInfo(contestInfo, run, oldRuns.getOrNull(index), problemId, teamId)
        }.filter {
            it.time <= timeBound
        }
    }

    private fun parseRunInfo(contestInfo: PCMSContestInfo, element: Element, oldRun: PCMSRunInfo?, problemId: Int, teamId: Int): PCMSRunInfo {
        val time = element.attr("time").toLong()
        val isFrozen = time >= contestInfo.freezeTime
        val isJudged = !isFrozen && "undefined" != element.attr("outcome")
        val result = when {
            !isJudged -> ""
            "yes" == element.attr("accepted") -> "AC"
            else -> outcomeMap.getOrDefault(
                element.attr("outcome"), "WA"
            )
        }
        return PCMSRunInfo(
            oldRun?.id ?: lastRunId++,
            isJudged, result, problemId, time, teamId,
            if (isJudged == oldRun?.isJudged && result == oldRun.result)
                oldRun.lastUpdateTime
            else
                contestInfo.contestTime
        )
    }

    var contestData: PCMSContestInfo
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
            var shortName = participant.attr("shortname")
            if (shortName.isEmpty()) {
                var index = participantName.indexOf("(")
                shortName = participantName.substring(0, index - 1)
                index = -1 //shortName.indexOf(",");
                shortName = shortName.substring(if (index == -1) 0 else index + 2)
                if (shortName.length >= 30) {
                    shortName = shortName.substring(0, 27) + "..."
                }
            }
            var region = participant.attr("region")
            if (region.isEmpty()) {
                val index = participantName.indexOf(",")
                if (index != -1) region = participantName.substring(0, index)
            }
            val hashTag = participant.attr("hashtag")
            val groups = mutableSetOf(region)
            PCMSTeamInfo(
                index, alias, hallId, participantName, shortName,
                hashTag, groups, problemInfo.size, 0
            )
        }
        contestData = PCMSContestInfo(problemInfo, teams)
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