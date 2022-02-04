package org.icpclive.cds.pcms

import org.icpclive.Config.loadFile
import org.icpclive.Config.loadProperties
import org.icpclive.DataBus.publishContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.cds.EventsLoader
import org.icpclive.cds.NetworkUtils.openAuthorizedStream
import org.icpclive.cds.ProblemInfo
import org.icpclive.cds.TeamInfo
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.DateTimeException
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

class PCMSEventsLoader : EventsLoader() {
    @Throws(IOException::class)
    fun loadProblemsInfo(problemsFile: String?) {
        val xml = loadFile(problemsFile!!)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val problems = doc.child(0)
        contestInfo.get().problems.clear()
        for (element in problems.children()) {
            val problem = ProblemInfo(
                element.attr("alias"),
                element.attr("name"),
                if (element.attr("color").isEmpty()) Color.BLACK else Color.decode(element.attr("color"))
            )
            contestInfo.get().problems.add(problem)
        }
    }

    var initialStandings: List<TeamInfo>

    @Throws(IOException::class)
    private fun updateStatements() {
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
                    updateStatements()
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
            contestInfo.set(updatedContestInfo)
        } else {
            element.children().forEach { element: Element -> parseAndUpdateStandings(element) }
        }
    }

    private var lastRunId = 0
    private fun parseContestInfo(element: Element): PCMSContestInfo {
        val problemsNumber = properties.getProperty("problems.number").toInt()
        val updatedContestInfo = PCMSContestInfo(problemsNumber)
        val previousStartTime = contestInfo.get().startTime
        val previousStatus = contestInfo.get().status
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
                ContestStatus.PAUSED -> if (previousStatus !== ContestStatus.PAUSED) {
                    updatedContestInfo.startTime = previousStartTime
                    updatedContestInfo.status = ContestStatus.RUNNING
                    updatedContestInfo.status = ContestStatus.PAUSED
                } else {
                    updatedContestInfo.contestTime = contestInfo.get().contestTime
                }
            }
        }
        updatedContestInfo.frozen = "yes" == element.attr("frozen")
        updatedContestInfo.problems.clear()
        updatedContestInfo.problems.addAll(contestInfo.get().problems)
        val standings = contestInfo.get().standings
        val taken = BooleanArray(standings.size)
        element.children().forEach { session: Element ->
            if ("session" == session.tagName()) {
                val teamInfo = parseTeamInfo(session, updatedContestInfo.contestTime)
                if (teamInfo != null) {
                    updatedContestInfo.addTeamStandings(teamInfo)
                    taken[teamInfo.id] = true
                }
            }
        }
        for (i in taken.indices) {
            if (!taken[i]) {
                updatedContestInfo.addTeamStandings(initialStandings[i] as PCMSTeamInfo)
            }
        }
        updatedContestInfo.fillTimeFirstSolved()
        updatedContestInfo.calculateRanks()
        updatedContestInfo.makeRuns()
        return updatedContestInfo
    }

    private fun parseTeamInfo(element: Element, timeBound: Long): PCMSTeamInfo? {
        val alias = element.attr("alias")
        val teamInfo = contestInfo.get().getParticipant(alias) ?: return null
        val teamInfoCopy = PCMSTeamInfo(teamInfo)
        teamInfoCopy.solvedProblemsNumber = element.attr("solved").toInt()
        teamInfoCopy.penalty = element.attr("penalty").toInt()
        for (i in element.children().indices) {
            val problemRuns = parseProblemRuns(element.child(i), i, teamInfoCopy.id, timeBound)
            lastRunId = teamInfoCopy.mergeRuns(problemRuns, i, lastRunId, timeBound)
        }
        return teamInfoCopy
    }

    private fun parseProblemRuns(element: Element, problemId: Int, teamId: Int, timeBound: Long): List<PCMSRunInfo> {
        if (contestInfo.get().status === ContestStatus.BEFORE) {
            return emptyList()
        }
        return element.children().map { run: Element ->
            parseRunInfo(run, problemId, teamId)
        }.filter {
            it.time <= timeBound
        }
    }

    private fun parseRunInfo(element: Element, problemId: Int, teamId: Int): PCMSRunInfo {
        val time = element.attr("time").toLong()
        val isFrozen = time >= contestInfo.get().freezeTime
        val isJudged = !isFrozen && "undefined" != element.attr("outcome")
        val result = if ("yes" == element.attr("accepted")) "AC" else if (!isJudged) "" else outcomeMap.getOrDefault(
            element.attr("outcome"), "WA"
        )
        return PCMSRunInfo(isJudged, result, problemId, time, teamId)
    }

    override val contestData: PCMSContestInfo
        get() = contestInfo.get()
    var contestInfo = AtomicReference<PCMSContestInfo>()
    private val properties: Properties

    init {
        properties = loadProperties("events")
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
        val problemsNumber = properties.getProperty("problems.number").toInt()
        val initial = PCMSContestInfo(problemsNumber)
        initial.contestLength = properties.getProperty("contest.length", "" + 5 * 60 * 60 * 1000).toInt()
        initial.freezeTime = properties.getProperty("freeze.time", "" + 4 * 60 * 60 * 1000).toInt()
        val fn = properties.getProperty("teams.url")
        val xml = loadFile(fn)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val participants = doc.child(0)
        for ((id, participant) in participants.children().withIndex()) {
            val participantName = participant.attr("name")
            val alias = participant.attr("id")
            var hallId = participant.attr("hall_id")
            if (hallId.length == 0) {
                hallId = alias
            }
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
            if (region.isNotEmpty()) {
                contestInfo.get().groups.add(region)
            }
            val groups = HashSet<String>()
            groups.add(region)
            val team = PCMSTeamInfo(
                id, alias, hallId, participantName, shortName,
                hashTag, groups, initial.problemsNumber, 0
            )
            if (team.shortName.isNotEmpty()) {
                initial.addTeamStandings(team)
            }
        }
        initialStandings = initial.standings
        contestInfo.set(initial)
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