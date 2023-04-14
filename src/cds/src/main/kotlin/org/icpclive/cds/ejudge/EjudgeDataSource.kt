package org.icpclive.cds.ejudge

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.common.xmlLoader
import org.icpclive.util.child
import org.icpclive.util.children
import org.icpclive.util.guessDatetimeFormat
import org.w3c.dom.Element
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class EjudgeDataSource(val properties: Properties) : FullReloadContestDataSource(5.seconds) {
    override val resultType =
        ContestResultType.valueOf(properties.getProperty("standings.resultType", "ICPC").uppercase())

    override suspend fun loadOnce(): ContestParseResult {
        val element = xmlLoader.load()
        return parseContestInfo(element.documentElement)
    }

    private fun parseProblemsInfo(doc: Element) = doc
        .child("problems")
        .children().mapIndexed { index, element ->
            ProblemInfo(
                letter = element.getAttribute("short_name"),
                name = element.getAttribute("long_name"),
                id = element.getAttribute("id").toInt(),
                ordinal = index,
                cdsId = element.getAttribute("id"),
                minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
                scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
            )
        }.toList()

    private fun parseTeamsInfo(doc: Element) = doc
        .child("users")
        .children().mapIndexed { index, participant ->
            val participantName = participant.getAttribute("name")
            TeamInfo(
                id = index,
                name = participantName,
                shortName = participantName,
                contestSystemId = participant.getAttribute("id"),
                groups = listOf(),
                hashTag = null,
                medias = emptyMap()
            )
        }.toList()

    private fun parseEjudgeTime(time: String): Instant {
        val formattedTime = time
            .replace("/", "-")
            .replace(" ", "T")
        return guessDatetimeFormat(formattedTime)
    }

    private fun parseContestInfo(element: Element): ContestParseResult {
        val contestLength = element.getAttribute("duration").toLong().seconds
        val startTime = element.getAttribute("start_time").ifEmpty {
            element.getAttribute("sched_start_time").ifEmpty { null }
        }?.let { parseEjudgeTime(it) } ?: Instant.fromEpochMilliseconds(0)
        val currentTime = parseEjudgeTime(element.getAttribute("current_time"))
        val name = element.child("name").textContent

        val status = when {
            currentTime >= startTime + contestLength -> ContestStatus.OVER
            currentTime < startTime -> ContestStatus.BEFORE
            else -> ContestStatus.RUNNING
        }

        var freezeTime = if (resultType == ContestResultType.ICPC) 4.hours else contestLength
        if (element.hasAttribute("fog_time")) {
            freezeTime = contestLength - element.getAttribute("fog_time").toLong().seconds
        }

        val teams = parseTeamsInfo(element)
        val teamIdMapping = teams.associateBy({ it.contestSystemId }, { it.id })

        return ContestParseResult(
            contestInfo = ContestInfo(
                name = name,
                status = status,
                resultType = resultType,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problems = parseProblemsInfo(element),
                teams = teams
            ),
            runs = element.child("runs").children().mapNotNull { run ->
                parseRunInfo(run, currentTime - startTime, teamIdMapping)
            }.toList(),
            emptyList()
        )
    }

    private fun parseRunInfo(
        element: Element,
        currentTime: Duration,
        teamIdMapping: Map<String, Int>
    ): RunInfo? {
        val time = element.getAttribute("time").toLong().seconds + element.getAttribute("nsec").toLong().nanoseconds
        if (time > currentTime) {
            return null
        }

        val teamId = teamIdMapping[element.getAttribute("user_id")]!!
        val runId = element.getAttribute("run_id").toInt()

        val result = statusMap.getOrDefault(element.getAttribute("status"), "WA")
        val percentage = when (result) {
            "" -> 0.0
            else -> 1.0
        }

        return RunInfo(
            id = runId,
            when (resultType) {
                ContestResultType.ICPC -> ICPCRunResult(
                    isAccepted = "AC" == result,
                    isAddingPenalty = "AC" != result && "CE" != result,
                    result = result,
                    isFirstToSolveRun = false
                ).takeIf { result != "" }

                ContestResultType.IOI -> {
                    val score = element.getAttribute("score").ifEmpty { "0" }.toDouble()
                    IOIRunResult(
                        score = listOf(score),
                    )
                }
            },
            problemId = element.getAttribute("prob_id").toInt(),
            teamId = teamId,
            percentage = percentage,
            time = time,
        )
    }

    private val xmlLoader = xmlLoader { properties.getProperty("url") }

    companion object {
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
