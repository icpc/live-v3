package org.icpclive.cds.yandex

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.yandex.api.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexContestInfo private constructor(
    private val startTime: Instant,
    private val duration: Duration,
    private val freezeTime: Duration,
    private val problems: List<ProblemInfo>,
    private val teams: List<TeamInfo>,
    private val testCountByProblem: List<Int?>
) {
    private val teamIds: Set<Int> = teams.map(TeamInfo::id).toSet()

    constructor(
        contestDescription: ContestDescription,
        problems: List<Problem>,
        participants: List<Participant>
    ) : this(
        Instant.parse(contestDescription.startTime),
        contestDescription.duration.seconds,
        (contestDescription.freezeTime ?: contestDescription.duration).seconds,
        problems.mapIndexed { index, it -> ProblemInfo(it.alias, it.name, null, index, index) },
        participants.map(Participant::toTeamInfo).sortedBy { it.id },
        problems.map(Problem::testCount)
    )

    fun submissionToRun(submission: Submission): RunInfo {
        val problemId = problems.indexOfFirst { it.letter == submission.problemAlias }
        if (problemId == -1) {
            throw IllegalStateException("Problem not found: ${submission.problemAlias}")
        }
        val testCount = testCountByProblem[problemId]

        if (submission.timeFromStart >= freezeTime) {
            return RunInfo(
                id = submission.id.toInt(),
                isAccepted = false,
                isJudged = false,
                isAddingPenalty = false,
                result = "",
                score = 0,
                problemId = problemId,
                teamId = submission.authorId.toInt(),
                percentage = 0.0,
                time = submission.timeFromStart,
            )
        }

        val result = getResult(submission.verdict)
        return RunInfo(
            id = submission.id.toInt(),
            isAccepted = result == "OK",
            isJudged = result != "",
            isAddingPenalty = result !in listOf("OK", "CE", ""),
            result = result,
            score = 0,
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
        resultType = ContestResultType.BINARY,
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

