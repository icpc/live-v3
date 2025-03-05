package org.icpclive.cds.plugins.ejudge

import kotlinx.datetime.*
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.child
import org.icpclive.cds.util.childOrNull
import org.icpclive.cds.util.children
import org.w3c.dom.Element
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Builder("ejudge")
public sealed interface EjudgeSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val source: UrlOrLocalPath
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC
    public val timeZone: TimeZone
        get() = TimeZone.of("Europe/Moscow")
    public val problemScoreLimit: Map<String, Double>
        get() = emptyMap()

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
                id = element.getAttribute("id").toProblemId(),
                displayName = element.getAttribute("short_name"),
                fullName = element.getAttribute("long_name"),
                ordinal = index,
                minScore = if (settings.resultType == ContestResultType.IOI) 0.0 else null,
                maxScore = if (settings.resultType == ContestResultType.IOI) 100.0 else null,
                scoreMergeMode = if (settings.resultType == ContestResultType.IOI) ScoreMergeMode.MAX_PER_GROUP else null
            )
        }.toList()

    private fun parseTeamsInfo(doc: Element) = doc
        .child("users")
        .children().mapIndexed { index, participant ->
            val participantName = participant.getAttribute("name")
            TeamInfo(
                id = participant.getAttribute("id").toTeamId(),
                fullName = participantName,
                displayName = participantName,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isOutOfContest = false,
                isHidden = false,
                organizationId = null
            )
        }.toList()

    private val timePattern = LocalDateTime.Format {
        year()
        alternativeParsing({ char('/') }) { char('-') }
        monthNumber()
        alternativeParsing({ char('/') }) { char('-') }
        dayOfMonth()
        char(' ')
        time(LocalTime.Formats.ISO)
    }

    private fun parseEjudgeTime(time: String): Instant {
        return LocalDateTime.parse(
            time,
            timePattern
        ).toInstant(settings.timeZone)
    }

    private fun parseContestInfo(element: Element): ContestParseResult {
        val contestLength = element.getAttribute("duration").toLong().seconds
        val startTime = element.getAttribute("start_time").ifEmpty {
            element.getAttribute("sched_start_time").ifEmpty { null }
        }?.let { parseEjudgeTime(it) } ?: Instant.fromEpochMilliseconds(0)
        val currentTime = parseEjudgeTime(element.getAttribute("current_time"))
        val name = element.child("name").textContent

        val freezeTime = when {
            element.hasAttribute("fog_time") -> contestLength - element.getAttribute("fog_time").toLong().seconds
            settings.resultType == ContestResultType.ICPC -> 4.hours
            else -> null
        }
        val teams = parseTeamsInfo(element)

        val userStartTime = element.childOrNull("userrunheaders")?.children()?.mapNotNull {
            val userId = it.getAttribute("user_id").ifEmpty { null } ?: return@mapNotNull null
            val userStartTime = it.getAttribute("start_time").ifEmpty { null } ?: return@mapNotNull null
            userId.toTeamId() to (parseEjudgeTime(userStartTime) - startTime)
        }?.toMap() ?: emptyMap()

        val problems = parseProblemsInfo(element)
        return ContestParseResult(
            contestInfo = ContestInfo(
                name = name,
                resultType = settings.resultType,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = problems,
                teamList = teams,
                groupList = emptyList(),
                organizationList = emptyList(),
                languagesList = emptyList(),
                penaltyRoundingMode = when (settings.resultType) {
                    ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                    ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
                }
            ),
            runs = element.child("runs").children().mapNotNull { run ->
                parseRunInfo(
                    run,
                    currentTime - startTime,
                    userStartTime,
                    settings.problemScoreLimit
                )
            }.toList(),
            emptyList(),
        )
    }

    private fun parseRunInfo(
        element: Element,
        currentTime: Duration,
        userStartTime: Map<TeamId, Duration>,
        problemScoreLimit: Map<String, Double>,
    ): RunInfo? {
        val time = element.getAttribute("time").toLong().seconds + element.getAttribute("nsec").toLong().nanoseconds
        if (time > currentTime) {
            return null
        }

        val teamId = element.getAttribute("user_id").toTeamId()
        val runId = element.getAttribute("run_id").toRunId()

        val result = when (element.getAttribute("status")) {
            "OK", "PT" -> Verdict.Accepted
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

        val problemId = element.getAttribute("prob_id")

        val runResult = if (result == null) RunResult.InProgress(0.0) else when (settings.resultType) {
            ContestResultType.ICPC -> result.toICPCRunResult()
            ContestResultType.IOI -> {
                if (result != Verdict.Accepted) {
                    RunResult.IOI(score = listOf(0.0), wrongVerdict = result)
                } else {
                    val result = element.getAttribute("group_scores").ifEmpty { element.getAttribute("score") }.ifEmpty { "0.0" }
                    RunResult.IOI(
                        score = result.split(" ").map { maxOf(0.0, minOf(it.toDouble(), problemScoreLimit[problemId] ?: Double.POSITIVE_INFINITY)) }
                    )
                }
            }
        }

        return RunInfo(
            id = runId,
            result = runResult,
            problemId = problemId.toProblemId(),
            teamId = teamId,
            time = time - (userStartTime[teamId] ?: Duration.ZERO),
            languageId = null,
        )
    }

    private val xmlLoader = DataLoader.xml(settings.network, settings.source)
}
