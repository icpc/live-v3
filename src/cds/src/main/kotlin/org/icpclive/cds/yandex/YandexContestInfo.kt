package org.icpclive.cds.yandex

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.yandex.api.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private fun Problem.toApi(index:Int, resultType: ContestResultType) = ProblemInfo(
    letter = alias,
    name = name,
    id = index,
    ordinal = index,
    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
)

class YandexContestInfo private constructor(
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
        resultType: ContestResultType
    ) : this(
        Instant.parse(contestDescription.startTime),
        contestDescription.duration.seconds,
        (contestDescription.freezeTime ?: contestDescription.duration).seconds,
        problems.mapIndexed { index, it -> it.toApi(index, resultType) },
        participants.map(Participant::toTeamInfo).sortedBy { it.id },
        problems.map(Problem::testCount),
        resultType
    )

    fun submissionToRun(submission: Submission): RunInfo {
        val problemId = problems.indexOfFirst { it.letter == submission.problemAlias }
        if (problemId == -1) {
            throw IllegalStateException("Problem not found: ${submission.problemAlias}")
        }
        val testCount = testCountByProblem[problemId]

        val result = getResult(submission.verdict)
        return RunInfo(
            id = submission.id.toInt(),
            result = when (resultType) {
                ContestResultType.ICPC -> ICPCRunResult(
                    isAccepted = result == "OK",
                    isAddingPenalty = result !in listOf("OK", "CE"),
                    result = result,
                    isFirstToSolveRun = false
                )
                ContestResultType.IOI -> IOIRunResult(
                    score = listOf(submission.score ?: 0.0),
                )
            }.takeIf { result != "" },
            problemId = problemId,
            teamId = submission.authorId.toInt(),
            percentage = when {
                result != "" -> 100.0
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
        status = deduceStatus(startTime, duration),
        resultType = resultType,
        startTime = startTime,
        contestLength = duration,
        freezeTime = freezeTime,
        problems = problems,
        teams = teams,
        penaltyRoundingMode = PenaltyRoundingMode.SUM_DOWN_TO_MINUTE
    )

    companion object {
        // There is no way to fetch YC server time, so here we go
        fun deduceStatus(startTime: Instant, duration: Duration): ContestStatus {
            val now = Clock.System.now()

            return when {
                now < startTime -> ContestStatus.BEFORE
                now < startTime + duration -> ContestStatus.RUNNING
                else -> ContestStatus.OVER
            }
        }
    }
}

