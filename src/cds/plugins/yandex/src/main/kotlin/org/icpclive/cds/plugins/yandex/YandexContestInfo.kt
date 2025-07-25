package org.icpclive.cds.plugins.yandex

import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.yandex.api.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private fun Problem.toApi(index: Int, resultType: ContestResultType) = ProblemInfo(
    id = id.toProblemId(),
    displayName = alias,
    fullName = name,
    ordinal = index,
    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
    maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
)

internal class YandexContestInfo(
    private val loginRegex: Regex,
    contestDescription: ContestDescription,
    problems: List<Problem>,
    participants: List<Participant>,
    private val resultType: ContestResultType,
) {

    private val name = contestDescription.name
    private val startTime = Instant.parse(contestDescription.startTime)
    private val duration = contestDescription.duration.seconds
    private val freezeTime = (contestDescription.freezeTime ?: contestDescription.duration).seconds
    private val problems = problems.sortedBy { it.alias }.mapIndexed { index, it -> it.toApi(index, resultType) }
    private val teams =  participants.map { it.toTeamInfo(loginRegex) }.sortedBy { it.id.value }
    private val teamIds = participants.associate { it.id to it.login }
    private val testCountByProblem = problems.associate { it.id to it.testCount }


    fun submissionToRun(submission: Submission): RunInfo {
        val testCount = testCountByProblem[submission.problemId]

        val verdict = getResult(submission.verdict)
        val result = if (verdict != null) {
            when (resultType) {
                ContestResultType.ICPC -> verdict.toICPCRunResult()
                ContestResultType.IOI -> RunResult.IOI(
                    score = listOf(submission.score ?: 0.0),
                )
            }
        } else {
            RunResult.InProgress(
                when {
                    testCount == null || testCount == 0 -> 0.0
                    submission.test == -1L -> 0.0
                    submission.test >= testCount -> 100.0
                    else -> submission.test.toDouble() / testCount
                }
            )
        }
        return RunInfo(
            id = submission.id.toRunId(),
            result = result,
            problemId = submission.problemId.toProblemId(),
            teamId = teamIds[submission.authorId]!!.toTeamId(),
            time = submission.timeFromStart,
            languageId = null
        )
    }

    fun toApi() = ContestInfo(
        name = name,
        resultType = resultType,
        startTime = startTime,
        contestLength = duration,
        freezeTime = freezeTime,
        problemList = problems,
        teamList = teams,
        groupList = emptyList(),
        organizationList = emptyList(),
        languagesList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.SUM_DOWN_TO_MINUTE
    )
}

