package org.icpclive.cds.plugins.yandex

import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.yandex.api.*
import org.icpclive.cds.plugins.yandex.api.Participant
import org.icpclive.cds.plugins.yandex.api.Problem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun Problem.toApi(index: Int, resultType: ContestResultType) = ProblemInfo(
    displayName = alias,
    fullName = name,
    id = index,
    ordinal = index,
    contestSystemId = id,
    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
)

internal class YandexContestInfo private constructor(
    private val name: String,
    private val startTime: Instant,
    private val duration: Duration,
    private val freezeTime: Duration,
    private val problems: List<ProblemInfo>,
    private val teams: List<TeamInfo>,
    private val testCountByProblem: List<Int?>,
    private val resultType: ContestResultType,
) {
    private val teamIds: Set<Int> = teams.map(TeamInfo::id).toSet()

    constructor(
        contestDescription: ContestDescription,
        problems: List<Problem>,
        participants: List<Participant>,
        resultType: ContestResultType,
    ) : this(
        contestDescription.name,
        Instant.parse(contestDescription.startTime),
        contestDescription.duration.seconds,
        (contestDescription.freezeTime ?: contestDescription.duration).seconds,
        problems.mapIndexed { index, it -> it.toApi(index, resultType) },
        participants.map(Participant::toTeamInfo).sortedBy { it.id },
        problems.map(Problem::testCount),
        resultType
    )

    fun submissionToRun(submission: Submission): RunInfo {
        val problemId = problems.indexOfFirst { it.displayName == submission.problemAlias }
        if (problemId == -1) {
            throw IllegalStateException("Problem not found: ${submission.problemAlias}")
        }
        val testCount = testCountByProblem[problemId]

        val result = getResult(submission.verdict)
        return RunInfo(
            id = submission.id.toInt(),
            result = when (resultType) {
                ContestResultType.ICPC -> result?.toRunResult()
                ContestResultType.IOI -> IOIRunResult(
                    score = listOf(submission.score ?: 0.0),
                )
            }.takeIf { result != null },
            problemId = problemId,
            teamId = submission.authorId.toInt(),
            percentage = when {
                result != null -> 100.0
                testCount == null || testCount == 0 -> 0.0
                submission.test == -1L -> 0.0
                submission.test >= testCount -> 100.0
                else -> submission.test.toDouble() / testCount
            },
            time = submission.timeFromStart,
        )
    }

    fun isTeamSubmission(submission: Submission): Boolean {
        return submission.authorId.toInt() in teamIds
    }

    fun toApi() = ContestInfo(
        name = name,
        status = ContestStatus.byCurrentTime(startTime, duration),
        resultType = resultType,
        startTime = startTime,
        contestLength = duration,
        freezeTime = freezeTime,
        problemList = problems,
        teamList = teams,
        groupList = emptyList(),
        organizationList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.SUM_DOWN_TO_MINUTE
    )
}

