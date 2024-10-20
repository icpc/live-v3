package org.icpclive.cds.plugins.pcms

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.api.RunResult
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
    public val jobsSources: UrlOrLocalPath?
        get() = null
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC
    public val allowResultsFromJobs: Boolean
        get() = false

    override fun toDataSource(): ContestDataSource = PCMSDataSource(this)
}

internal class PCMSDataSource(val settings: PCMSSettings) : FullReloadContestDataSource(if (settings.jobsSources != null) 500.milliseconds else 5.seconds) {
    private val dataLoader = DataLoader.xml(settings.network) { settings.source }.cached(5.seconds)
    private val jobsDataLoader = settings.jobsSources?.let { DataLoader.xml(settings.network) { it } }

    private val resultType = settings.resultType
    private var startTime = Instant.fromEpochMilliseconds(0)
    private val maxTestsPerProblem = mutableMapOf<ProblemId, Int>()

    fun Element.attr(name: String) = getAttribute(name).takeIf { it.isNotEmpty() }


    override suspend fun loadOnce(): ContestParseResult {
        return parseAndUpdateStandings(
            dataLoader.load().documentElement,
            jobsDataLoader?.load()?.documentElement
        )
    }

    private fun parseAndUpdateStandings(
        mainElement: Element,
        jobsElement: Element?
    ) =
        parseContestInfo(
            mainElement.child("contest"),
            jobsElement
        )

    private fun parseContestInfo(
        mainElement: Element,
        jobsElement: Element?
    ): ContestParseResult {
        val statusStr = mainElement.attr("status")!!
        val contestTime = mainElement.attr("time")!!.toLong().milliseconds
        val contestLength = mainElement.attr("length")!!.toInt().milliseconds
        if (statusStr != "before" && startTime.epochSeconds == 0L) {
            startTime = Clock.System.now() - contestTime
        }
        val freezeTime = if (resultType == ContestResultType.ICPC) contestLength - 1.hours else null
        val status = when (statusStr) {
            "before" -> ContestStatus.BEFORE(scheduledStartAt = startTime)
            "running" -> ContestStatus.RUNNING(startedAt = startTime, frozenAt = if (freezeTime != null && contestTime > freezeTime) startTime + freezeTime else null)
            "over" -> ContestStatus.OVER(startedAt = startTime, frozenAt = if (freezeTime != null && contestTime > freezeTime) startTime + freezeTime else null, finishedAt = startTime + contestLength)
            else -> error("Unknown contest status $statusStr")
        }

        val problemsElement = mainElement.child("challenge")

        val problems = problemsElement
            .children("problem")
            .mapIndexed { index, it ->
                ProblemInfo(
                    id = it.attr("alias")!!.toProblemId(),
                    displayName = it.attr("alias")!!,
                    fullName = it.attr("name")!!,
                    ordinal = index,
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_PER_GROUP else null
                )
            }.toList()

        val teamsAndRuns = mainElement
            .children("session")
            .map { it.parseTeamInfo(contestTime) }
            .toList()
        if (status is ContestStatus.RUNNING) {
            log.info { "Loaded contestInfo for time = $contestTime" }
        }
        val teams = teamsAndRuns.map { it.first }.sortedBy { it.id.value }
        val mainRuns = teamsAndRuns.flatMap { it.second }
        jobsElement?.let {
            for (job in it.children("job")) {
                val problem =  job.attr("problem-alias")?.toProblemId() ?: continue
                val testNo = job.attr("test-no")?.toIntOrNull() ?: continue
                maxTestsPerProblem[problem] = maxOf(maxTestsPerProblem[problem] ?: 0, testNo)
            }
        }
        val jobsRuns = jobsElement?.let {
            it.children("job")
                .filter { it.hasAttribute("problem-alias") }
                .map { parseRunFromJob(it) }
                .toList()
        } ?: emptyList()
        val runs = (mainRuns + jobsRuns).groupBy { it.id }.values.map {
            it.firstOrNull { it.result !is RunResult.InProgress } ?: it.first()
        }
        return ContestParseResult(
            ContestInfo(
                name = mainElement.attr("name")!!,
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

    private fun Element.parseTeamInfo(contestTime: Duration): Pair<TeamInfo, List<RunInfo>> {
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
            children("problem").flatMap { problem ->
                parseProblemRuns(
                    problem,
                    team.id,
                    problem.attr("alias")!!.toProblemId(),
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

    private fun parseRunFromJob(job: Element): RunInfo {
        val verdict = getVerdict(job)
        val testNo = job.getAttribute("test-no").toInt()
        val problemId = job.getAttribute("problem-alias").toProblemId()
        val problemTestNo = maxTestsPerProblem[problemId]
        return RunInfo(
            id = job.getAttribute("id").replaceAfterLast(".", "").removeSuffix(".").toRunId(),
            result = when (resultType) {
                ContestResultType.IOI -> {
                    when (verdict) {
                        null -> null
                        Verdict.Accepted -> if (settings.allowResultsFromJobs) RunResult.IOI(listOf(job.attr("score")!!.toDouble())) else RunResult.InProgress(1.0)
                        else -> RunResult.IOI(score = emptyList(), wrongVerdict = verdict)
                    }
                }
                ContestResultType.ICPC -> when (verdict) {
                    null -> null
                    else -> if (settings.allowResultsFromJobs) verdict.toICPCRunResult() else RunResult.InProgress(1.0)
                }
            } ?: run {
                RunResult.InProgress(if (problemTestNo == null) 0.0 else testNo.toDouble() / problemTestNo)
            },
            problemId = problemId,
            teamId = job.getAttribute("party-alias").toTeamId(),
            time = job.getAttribute("time").toLong().milliseconds,
            languageId = job.getAttribute("language-id").takeIf { it.isNotEmpty() }?.toLanguageId()
        )
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
