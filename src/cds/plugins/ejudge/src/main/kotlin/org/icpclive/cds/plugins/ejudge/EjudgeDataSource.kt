package org.icpclive.cds.plugins.ejudge

import kotlinx.datetime.*
import org.icpclive.cds.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.ksp.Builder
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.util.child
import org.icpclive.util.children
import org.w3c.dom.Element
import java.time.format.DateTimeFormatter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Builder("ejudge")
public sealed interface EjudgeSettings : CDSSettings {
    public val url: UrlOrLocalPath
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC
    public val timeZone: TimeZone
        get() = TimeZone.of("Europe/Moscow")

    override fun toDataSource(): ContestDataSource = EjudgeDataSource(this)
}

internal class EjudgeDataSource(val settings: EjudgeSettings) : FullReloadContestDataSource(5.seconds) {
    override suspend fun loadOnce(): ContestParseResult {
        val element = xmlLoader.load()
        return parseContestInfo(element.documentElement)
    }

    private fun parseProblemsInfo(doc: Element) = doc
        .child("problems")
        .children().mapIndexed { index, element ->
            ProblemInfo(
                displayName = element.getAttribute("short_name"),
                fullName = element.getAttribute("long_name"),
                id = element.getAttribute("id").toInt(),
                ordinal = index,
                contestSystemId = element.getAttribute("id"),
                minScore = if (settings.resultType == ContestResultType.IOI) 0.0 else null,
                maxScore = if (settings.resultType == ContestResultType.IOI) 100.0 else null,
                scoreMergeMode = if (settings.resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
            )
        }.toList()

    private fun parseTeamsInfo(doc: Element) = doc
        .child("users")
        .children().mapIndexed { index, participant ->
            val participantName = participant.getAttribute("name")
            TeamInfo(
                id = index,
                fullName = participantName,
                displayName = participantName,
                contestSystemId = participant.getAttribute("id"),
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isOutOfContest = false,
                isHidden = false,
                organizationId = null
            )
        }.toList()

    private val timePattern: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private fun parseEjudgeTime(time: String): Instant {
        return java.time.LocalDateTime.parse(
            time.replace("/", "-"), // snark's ejudge uses '/' instead of '-'
            timePattern
        ).toKotlinLocalDateTime()
            .toInstant(settings.timeZone)
    }

    private fun parseContestInfo(element: Element): ContestParseResult {
        val contestLength = element.getAttribute("duration").toLong().seconds
        val startTime = element.getAttribute("start_time").ifEmpty {
            element.getAttribute("sched_start_time").ifEmpty { null }
        }?.let { parseEjudgeTime(it) } ?: Instant.fromEpochMilliseconds(0)
        val currentTime = parseEjudgeTime(element.getAttribute("current_time"))
        val name = element.child("name").textContent

        val status = ContestStatus.byCurrentTime(startTime, contestLength)

        var freezeTime = if (settings.resultType == ContestResultType.ICPC) 4.hours else contestLength
        if (element.hasAttribute("fog_time")) {
            freezeTime = contestLength - element.getAttribute("fog_time").toLong().seconds
        }

        val teams = parseTeamsInfo(element)
        val teamIdMapping = teams.associateBy({ it.contestSystemId }, { it.id })

        return ContestParseResult(
            contestInfo = ContestInfo(
                name = name,
                status = status,
                resultType = settings.resultType,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = parseProblemsInfo(element),
                teamList = teams,
                groupList = emptyList(),
                organizationList = emptyList(),
                penaltyRoundingMode = when (settings.resultType) {
                    ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                    ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
                }
            ),
            runs = element.child("runs").children().mapNotNull { run ->
                parseRunInfo(run, currentTime - startTime, teamIdMapping)
            }.toList(),
            emptyList(),
        )
    }

    private fun parseRunInfo(
        element: Element,
        currentTime: Duration,
        teamIdMapping: Map<String, Int>,
    ): RunInfo? {
        val time = element.getAttribute("time").toLong().seconds + element.getAttribute("nsec").toLong().nanoseconds
        if (time > currentTime) {
            return null
        }

        val teamId = teamIdMapping[element.getAttribute("user_id")]!!
        val runId = element.getAttribute("run_id").toInt()

        val result = when (element.getAttribute("status")) {
            "OK" -> Verdict.Accepted
            "CE" -> Verdict.CompilationError
            "RT" -> Verdict.RuntimeError
            "TL" -> Verdict.TimeLimitExceeded
            "PE" -> Verdict.PresentationError
            "WA" -> Verdict.WrongAnswer
            "CF" -> Verdict.Fail
            "IG" -> Verdict.Ignored
            "DQ" -> Verdict.Ignored
            "ML" -> Verdict.MemoryLimitExceeded
            "SE" -> Verdict.SecurityViolation
            "SV" -> Verdict.Ignored
            "WT" -> Verdict.IdlenessLimitExceeded
            "RJ" -> Verdict.Ignored
            "SK" -> Verdict.Ignored
            else -> null
        }

        return RunInfo(
            id = runId,
            when (settings.resultType) {
                ContestResultType.ICPC -> result?.toICPCRunResult() ?: RunResult.InProgress(0.0)

                ContestResultType.IOI -> {
                    val score = element.getAttribute("score").ifEmpty { null }?.toDouble()
                    if (score != null) {
                        RunResult.IOI(
                            score = listOf(score),
                        )
                    } else {
                        RunResult.InProgress(0.0)
                    }
                }
            },
            problemId = element.getAttribute("prob_id").toInt(),
            teamId = teamId,
            time = time,
        )
    }

    private val xmlLoader = xmlLoader(networkSettings = settings.network) { settings.url }
}
