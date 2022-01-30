package org.icpclive.events.PCMS

import org.icpclive.Config.loadFile
import org.icpclive.Config.loadProperties
import org.icpclive.events.NetworkUtils.openAuthorizedStream
import org.icpclive.DataBus.publishContestInfo
import kotlin.Throws
import java.io.IOException
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.util.stream.Collectors
import java.lang.InterruptedException
import org.icpclive.api.ContestStatus
import org.icpclive.events.*
import java.util.Locale
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.slf4j.LoggerFactory
import java.awt.Color
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicReference
import java.util.Properties
import java.util.HashSet

class PCMSEventsLoader : EventsLoader() {
    @Throws(IOException::class)
    fun loadProblemsInfo(problemsFile: String?) {
        val xml = loadFile(problemsFile!!)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val problems = doc.child(0)
        contestInfo.get().problems = ArrayList()
        for (element in problems.children()) {
            val problem = ProblemInfo()
            problem.letter = element.attr("alias")
            problem.name = element.attr("name")
            problem.color = if (element.attr("color") == null) Color.BLACK else Color.decode(element.attr("color"))
            contestInfo.get().problems.add(problem)
        }
    }

    var initialStandings: Array<out TeamInfo>
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
            log.error("error", e)
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
                log.error("error", e)
            } catch (e: InterruptedException) {
                log.error("error", e)
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
        val currentTime = element.attr("time").toLong()
        val previousStatus = contestInfo.get().status
        updatedContestInfo.status = ContestStatus.valueOf(element.attr("status").uppercase(Locale.getDefault()))
        when (updatedContestInfo.status) {
            ContestStatus.BEFORE -> {}
            ContestStatus.RUNNING -> if (previousStatus !== ContestStatus.RUNNING || previousStartTime == 0L) {
                updatedContestInfo.startTime = System.currentTimeMillis() - currentTime
            } else {
                updatedContestInfo.startTime = previousStartTime
            }
            ContestStatus.PAUSED -> if (previousStatus !== ContestStatus.PAUSED) {
                updatedContestInfo.startTime = previousStartTime
                updatedContestInfo.status = ContestStatus.RUNNING
                updatedContestInfo.status = ContestStatus.PAUSED
            } else {
                updatedContestInfo.lastTime = contestInfo.get().lastTime
            }
        }
        updatedContestInfo.frozen = "yes" == element.attr("frozen")
        ContestInfo.CONTEST_LENGTH = ContestInfo.CONTEST_LENGTH
        ContestInfo.FREEZE_TIME = ContestInfo.CONTEST_LENGTH
        updatedContestInfo.problems = contestInfo.get().problems
        val standings = contestInfo.get().standings
        val taken = BooleanArray(standings.size)
        element.children().forEach { session: Element ->
            if ("session" == session.tagName()) {
                val teamInfo = parseTeamInfo(session)
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
        updatedContestInfo.lastRunId = lastRunId - 1
        updatedContestInfo.fillTimeFirstSolved()
        updatedContestInfo.calculateRanks()
        updatedContestInfo.makeRuns()
        return updatedContestInfo
    }

    private fun parseTeamInfo(element: Element): PCMSTeamInfo? {
        val alias = element.attr("alias")
        val teamInfo = contestInfo.get().getParticipant(alias)
        if (teamInfo == null || teamInfo.shortName.length == 0) {
            return null
        }
        val teamInfoCopy = PCMSTeamInfo(teamInfo)
        teamInfoCopy.solvedProblemsNumber = element.attr("solved").toInt()
        teamInfoCopy.penalty = element.attr("penalty").toInt()
        for (i in element.children().indices) {
            val problemRuns = parseProblemRuns(element.child(i), i, teamInfoCopy.id)
            lastRunId = teamInfoCopy.mergeRuns(problemRuns, i, lastRunId, contestInfo.get().currentTime)
        }
        return teamInfoCopy
    }

    private fun parseProblemRuns(element: Element, problemId: Int, teamId: Int): ArrayList<PCMSRunInfo> {
        val runs = ArrayList<PCMSRunInfo>()
        if (contestInfo.get().status === ContestStatus.BEFORE) {
            return runs
        }
        element.children().forEach{ run: Element ->
            val runInfo = parseRunInfo(run, problemId, teamId)
            runs.add(runInfo)
        }
        return runs
    }

    private fun parseRunInfo(element: Element, problemId: Int, teamId: Int): PCMSRunInfo {
        val time = element.attr("time").toLong()
        val timestamp = (contestInfo.get().startTime + time) / 1000
        val isFrozen = time >= ContestInfo.FREEZE_TIME
        val isJudged = !isFrozen && "undefined" != element.attr("outcome")
        val result = if ("yes" == element.attr("accepted")) "AC" else if (!isJudged) "" else outcomeMap.getOrDefault(
            element.attr("outcome"), "WA"
        )
        return PCMSRunInfo(isJudged, result, problemId, time, timestamp, teamId)
    }

    override val contestData: PCMSContestInfo
        get() = contestInfo.get()
    var contestInfo = AtomicReference<PCMSContestInfo>()
    private val properties: Properties

    init {
        properties = loadProperties("events")
        emulationSpeed = properties.getProperty("emulation.speed", "1").toDouble()
        ContestInfo.CONTEST_LENGTH = properties.getProperty("contest.length", "" + 5 * 60 * 60 * 1000).toInt()
        ContestInfo.FREEZE_TIME = properties.getProperty("freeze.time", "" + 4 * 60 * 60 * 1000).toInt()
        val problemsNumber = properties.getProperty("problems.number").toInt()
        val initial = PCMSContestInfo(problemsNumber)
        val fn = properties.getProperty("teams.url")
        val xml = loadFile(fn)
        val doc = Jsoup.parse(xml, "", Parser.xmlParser())
        val participants = doc.child(0)
        var id = 0
        for (participant in participants.children()) {
            val participantName = participant.attr("name")
            val alias = participant.attr("id")
            var hallId = participant.attr("hall_id")
            if (hallId.length == 0) {
                hallId = alias
            }
            var shortName = participant.attr("shortname")
            if (shortName == null || shortName.length == 0) {
                var index = participantName.indexOf("(")
                shortName = participantName.substring(0, index - 1)
                index = -1 //shortName.indexOf(",");
                shortName = shortName.substring(if (index == -1) 0 else index + 2)
                if (shortName.length >= 30) {
                    shortName = shortName.substring(0, 27) + "..."
                }
            }
            var region = participant.attr("region")
            if (region?.length == 0) {
                val index = participantName.indexOf(",")
                if (index != -1) region = participantName.substring(0, index)
            }
            val hashTag = participant.attr("hashtag")
            if (region != null && region?.length != 0) {
                ContestInfo.GROUPS.add(region)
            }
            val groups = HashSet<String>()
            groups.add(region)
            val team = PCMSTeamInfo(
                id, alias, hallId, participantName, shortName,
                hashTag, groups, initial.problemsNumber, 0
            )
            if (team.shortName.length != 0) {
                initial.addTeamStandings(team)
                id++
            }
        }
        initialStandings = initial.standings
        contestInfo.set(initial)
        loadProblemsInfo(properties.getProperty("problems.url"))
    }

    companion object {
        private val log = LoggerFactory.getLogger(PCMSEventsLoader::class.java)
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