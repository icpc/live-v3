package org.icpclive.cds.pcms

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.PCMSSettings
import org.icpclive.util.*
import org.w3c.dom.Element
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class PCMSDataSource(val settings: PCMSSettings) : FullReloadContestDataSource(5.seconds) {
    private val login = settings.login?.value
    private val password = settings.password?.value
    private val dataLoader = xmlLoader(
        networkSettings = settings.network,
        login?.let { ClientAuth.Basic(login, password!!) }) {
        settings.url.value
    }

    val resultType = settings.resultType
    val runIds = Enumerator<String>()
    val teamIds = Enumerator<String>()
    val problemIds = Enumerator<String>()
    var startTime = Instant.fromEpochMilliseconds(0)

    override suspend fun loadOnce() : ContestParseResult {
        val problemsOverride =  settings.problemsUrl?.let { loadCustomProblems(it.value) }

        return parseAndUpdateStandings(dataLoader.load().documentElement, problemsOverride)
    }
    private fun parseAndUpdateStandings(element: Element, problemsOverride: Element?) = parseContestInfo(element.child("contest"), problemsOverride)

    private suspend fun loadCustomProblems(problemsUrl: String) : Element {
        val problemsLoader = xmlLoader(networkSettings = settings.network) { problemsUrl }
        return problemsLoader.load().documentElement
    }

    private fun parseContestInfo(element: Element, problemsOverride: Element?) : ContestParseResult {
        val status = ContestStatus.valueOf(element.getAttribute("status").uppercase(Locale.getDefault()))
        val contestTime = element.getAttribute("time").toLong().milliseconds
        val contestLength = element.getAttribute("length").toInt().milliseconds
        if (status != ContestStatus.BEFORE && startTime.epochSeconds == 0L) {
            startTime = Clock.System.now() - contestTime
        }
        val freezeTime = if (resultType == ContestResultType.ICPC) contestLength - 1.hours else contestLength

        val problemsElement = problemsOverride ?: element.child("challenge")

        val problems = problemsElement
            .children("problem")
            .mapIndexed { index, it ->
                ProblemInfo(
                    displayName = it.getAttribute("alias"),
                    fullName = it.getAttribute("name"),
                    id = problemIds[it.getAttribute("alias")],
                    ordinal = index,
                    contestSystemId = it.getAttribute("id").takeIf { it.isNotEmpty() } ?: it.getAttribute("alias"),
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_PER_GROUP else null
                )
            }.toList()

        val teamsAndRuns = element
            .children("session")
            .map { parseTeamInfo(it, contestTime) }
            .toList()
        if (status == ContestStatus.RUNNING) {
            logger.info("Loaded contestInfo for time = $contestTime")
        }
        val teams = teamsAndRuns.map { it.first }.sortedBy { it.id }
        return ContestParseResult(
            ContestInfo(
                name = element.getAttribute("name"),
                status = status,
                resultType = resultType,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = problems,
                teamList = teams,
                groupList = teams.flatMap { it.groups }.distinct().map { GroupInfo(it, it, isHidden = false, isOutOfContest = false) },
                organizationList = emptyList(),
                penaltyRoundingMode = when (resultType) {
                    ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                    ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
                }
            ),
            teamsAndRuns.flatMap { it.second },
            emptyList()
        )
    }

    private fun parseTeamInfo(element: Element, contestTime: Duration) : Pair<TeamInfo, List<RunInfo>> {
        fun attr(name: String) = element.getAttribute(name).takeIf { it.isNotEmpty() }
        val alias = attr("alias")!!
        val team = TeamInfo(
            id = teamIds[alias],
            fullName = attr("party")!!,
            displayName = attr("shortname") ?: attr("party")!!,
            hashTag = attr("hashtag"),
            groups = attr("region")?.split(",") ?: emptyList(),
            medias = listOfNotNull(
                attr("screen")?.let { TeamMediaType.SCREEN to MediaType.Video(it) },
                attr("camera")?.let { TeamMediaType.CAMERA to MediaType.Video(it) },
                attr("record")?.let { TeamMediaType.RECORD to MediaType.Video(it) },
            ).associate { it },
            contestSystemId = alias,
            isOutOfContest = false,
            isHidden = false,
            organizationId = null
        )
        val runs =
            element.children("problem").flatMap { problem ->
                parseProblemRuns(
                    problem,
                    team.id,
                    problemIds[problem.getAttribute("alias")],
                    contestTime
                )
            }.toList()
        return team to runs
    }

    private fun parseProblemRuns(
        element: Element,
        teamId: Int,
        problemId: Int,
        contestTime: Duration,
    ): Sequence<RunInfo> {
        return element.children()
            .filter { it.getAttribute("time").toLong().milliseconds <= contestTime }
            .map { parseRunInfo(it, teamId, problemId) }
    }

    private fun parseRunInfo(
        element: Element,
        teamId: Int,
        problemId: Int,
    ): RunInfo {
        val time = element.getAttribute("time").toLong().milliseconds
        val id = runIds[element.getAttribute("run-id")]
        val verdict = getVerdict(element)

        return RunInfo(
            id = id,
            when (resultType) {
                ContestResultType.IOI -> {
                    when (verdict) {
                        null -> null
                        Verdict.Accepted -> {
                            val score = element.getAttribute("score").toDouble()
                            val groupsScore =
                                element.children("group").map { it.getAttribute("score").toDouble() }.toList()
                                    .takeIf { it.isNotEmpty() }

                            IOIRunResult(score = groupsScore ?: listOf(score))
                        }
                        else -> IOIRunResult(score = emptyList(), wrongVerdict = verdict)
                    }
                }
                ContestResultType.ICPC -> {
                    verdict?.toRunResult()
                }
            },
            problemId = problemId,
            teamId = teamId,
            percentage = if (verdict == null) 0.0 else 1.0,
            time = time,
        )
    }

    private fun getVerdict(element: Element) = when {
        "yes" == element.getAttribute("accepted") -> Verdict.Accepted
        else -> when (element.getAttribute("outcome")) {
            "fail" -> Verdict.Fail
            "unknown" -> null
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
            else -> Verdict.WrongAnswer
        }
    }

    companion object {
        private val logger = getLogger(PCMSDataSource::class)
    }
}
