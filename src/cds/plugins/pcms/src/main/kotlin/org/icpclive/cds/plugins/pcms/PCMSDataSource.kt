package org.icpclive.cds.plugins.pcms

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.settings.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.util.*
import org.w3c.dom.Element
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Builder("pcms")
public sealed interface PCMSSettings : CDSSettings {
    public val source: UrlOrLocalPath
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC

    override fun toDataSource(): ContestDataSource = PCMSDataSource(this)
}

internal class PCMSDataSource(val settings: PCMSSettings) : FullReloadContestDataSource(5.seconds) {
    private val dataLoader = DataLoader.xml(settings.network) { settings.source }

    private val resultType = settings.resultType
    private var startTime = Instant.fromEpochMilliseconds(0)

    override suspend fun loadOnce(): ContestParseResult {
        return parseAndUpdateStandings(dataLoader.load().documentElement)
    }

    private fun parseAndUpdateStandings(element: Element) =
        parseContestInfo(element.child("contest"))

    private suspend fun loadCustomProblems(problemsUrl: UrlOrLocalPath): Element {
        val problemsLoader = DataLoader.xml(networkSettings = settings.network) { problemsUrl }
        return problemsLoader.load().documentElement
    }

    private fun parseContestInfo(element: Element): ContestParseResult {
        val statusStr = element.getAttribute("status")
        val contestTime = element.getAttribute("time").toLong().milliseconds
        val contestLength = element.getAttribute("length").toInt().milliseconds
        if (statusStr != "before" && startTime.epochSeconds == 0L) {
            startTime = Clock.System.now() - contestTime
        }
        val freezeTime = if (resultType == ContestResultType.ICPC) contestLength - 1.hours else null
        val status = when (statusStr) {
            "before" -> ContestStatus.BEFORE(scheduledStartAt = startTime)
            "running" -> ContestStatus.RUNNING(startedAt = startTime, frozenAt = if (freezeTime != null && contestTime > freezeTime) startTime + freezeTime else null)
            "over" -> ContestStatus.OVER(startedAt = startTime, frozenAt = if (freezeTime != null && contestTime > freezeTime) startTime + freezeTime else null, finishedAt = startTime + contestLength)
            else -> error("Unknown contest status ${statusStr}")
        }

        val problemsElement = element.child("challenge")

        val problems = problemsElement
            .children("problem")
            .mapIndexed { index, it ->
                ProblemInfo(
                    id = (it.getAttribute("id").takeIf { it.isNotEmpty() } ?: it.getAttribute("alias")).toProblemId(),
                    displayName = it.getAttribute("alias"),
                    fullName = it.getAttribute("name"),
                    ordinal = index,
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_PER_GROUP else null
                )
            }.toList()

        val teamsAndRuns = element
            .children("session")
            .map { parseTeamInfo(it, contestTime) }
            .toList()
        if (status is ContestStatus.RUNNING) {
            log.info { "Loaded contestInfo for time = $contestTime" }
        }
        val teams = teamsAndRuns.map { it.first }.sortedBy { it.id.value }
        val runs = teamsAndRuns.flatMap { it.second }
        return ContestParseResult(
            ContestInfo(
                name = element.getAttribute("name"),
                status = status,
                resultType = resultType,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = problems,
                teamList = teams,
                groupList = teams.flatMap { it.groups }.distinct().map { GroupInfo(it, it.value, isHidden = false, isOutOfContest = false) },
                organizationList = emptyList(),
                languagesList = runs.languages(),
                penaltyRoundingMode = when (resultType) {
                    ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                    ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
                }
            ),
            runs,
            emptyList()
        )
    }

    private fun parseTeamInfo(element: Element, contestTime: Duration): Pair<TeamInfo, List<RunInfo>> {
        fun attr(name: String) = element.getAttribute(name).takeIf { it.isNotEmpty() }
        val alias = attr("alias")!!
        val team = TeamInfo(
            id = alias.toTeamId(),
            fullName = attr("party")!!,
            displayName = attr("shortname") ?: attr("party")!!,
            hashTag = attr("hashtag"),
            groups = attr("region")?.split(",")?.map { it.toGroupId() } ?: emptyList(),
            medias = listOfNotNull(
                attr("screen")?.let { TeamMediaType.SCREEN to MediaType.Video(it) },
                attr("camera")?.let { TeamMediaType.CAMERA to MediaType.Video(it) },
                attr("record")?.let { TeamMediaType.RECORD to MediaType.Video(it) },
            ).associate { it },
            isOutOfContest = false,
            isHidden = false,
            organizationId = null
        )
        val runs =
            element.children("problem").flatMap { problem ->
                parseProblemRuns(
                    problem,
                    team.id,
                    problem.getAttribute("alias").toProblemId(),
                    contestTime
                )
            }.toList()
        return team to runs
    }

    private fun parseProblemRuns(
        element: Element,
        teamId: TeamId,
        problemId: ProblemId,
        contestTime: Duration,
    ): Sequence<RunInfo> {
        return element.children()
            .filter { it.getAttribute("time").toLong().milliseconds <= contestTime }
            .mapIndexed { index, it -> parseRunInfo(it, teamId, problemId, index) }
    }

    private fun parseRunInfo(
        element: Element,
        teamId: TeamId,
        problemId: ProblemId,
        index: Int
    ): RunInfo {
        val time = element.getAttribute("time").toLong().milliseconds
        val id = element.getAttribute("run-id").takeIf { it.isNotEmpty() } ?: "$teamId-$problemId-$index"
        val verdict = getVerdict(element)

        return RunInfo(
            id = id.toRunId(),
            when (resultType) {
                ContestResultType.IOI -> {
                    when (verdict) {
                        null -> null
                        Verdict.Accepted -> {
                            val score = element.getAttribute("score").toDouble()
                            val groupsScore =
                                element.children("group").map { it.getAttribute("score").toDouble() }.toList()
                                    .takeIf { it.isNotEmpty() }

                            RunResult.IOI(score = groupsScore ?: listOf(score))
                        }

                        else -> RunResult.IOI(score = emptyList(), wrongVerdict = verdict)
                    }
                }

                ContestResultType.ICPC -> {
                    verdict?.toICPCRunResult()
                }
            } ?: RunResult.InProgress(0.0),
            problemId = problemId,
            teamId = teamId,
            time = time,
            languageId = element.getAttribute("language-id").takeIf { it.isNotEmpty() }?.toLanguageId()
        )
    }

    private fun getVerdict(element: Element) = when (element.getAttribute("accepted")) {
        "yes" -> Verdict.Accepted
        "undefined" -> null
        else -> when (element.getAttribute("outcome")) {
            "fail" -> Verdict.Fail
            "undefined" -> null
            "unknown" -> Verdict.Rejected
            "accepted" -> Verdict.Accepted
            "compilation-error" -> Verdict.CompilationError
            "wrong-answer" -> Verdict.WrongAnswer
            "presentation-error" -> Verdict.PresentationError
            "runtime-error" -> Verdict.RuntimeError
            "time-limit-exceeded" -> Verdict.TimeLimitExceeded
            "memory-limit-exceeded" -> Verdict.MemoryLimitExceeded
            "output-limit-exceeded" -> Verdict.OutputLimitExceeded
            "idleness-limit-exceeded" -> Verdict.IdlenessLimitExceeded
            "security-violation" -> Verdict.SecurityViolation
            else -> {
                log.error { "Unknown verdict ${element.getAttribute("outcome")}, assuming WrongAnswer" }
                Verdict.WrongAnswer
            }
        }
    }

    companion object {
        private val log by getLogger()
    }
}
