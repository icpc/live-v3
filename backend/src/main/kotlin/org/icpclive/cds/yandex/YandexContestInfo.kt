package org.icpclive.cds.yandex

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.ProblemInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.yandex.api.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class YandexContestInfo(
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
            problems.map(Problem::toProblemInfo),
            participants.map(Participant::toTeamInfo),
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
                problemId = problemId,
                teamId = submission.authorId.toInt(),
                percentage = 0.0,
                time = submission.timeFromStart,
                isFirstSolvedRun = false
            )
        }

        val result = getResult(submission.verdict)
        return RunInfo(
            id = submission.id.toInt(),
            isAccepted = result == "OK",
            isJudged = result != "",
            isAddingPenalty = result !in listOf("OK", "CE", ""),
            result = result,
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
            isFirstSolvedRun = false
        )
    }

    fun isTeamSubmission(submission: Submission): Boolean {
        return submission.authorId.toInt() in teamIds
    }

    fun toApi() = ContestInfo(
        status = deduceStatus(startTime, duration),
        startTime = startTime,
        contestLength = duration,
        freezeTime = freezeTime,
        problems = problems.map { it.toApi() },
        teams = teams.map { it.toApi() }.sortedBy { it.id },
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

