package org.icpclive.cds.clics.model

import kotlin.time.Duration

class ClicsRunInfo(
    val id: Int,
    val problem: ClicsProblemInfo,
    val teamId: Int,
    val submissionTime: Duration
) {
    val passedCaseRun = mutableSetOf<Int>()
    var judgementType: ClicsJudgementTypeInfo? = null

    fun toApi() = org.icpclive.api.RunInfo(
        id = id,
        isAccepted = judgementType?.isAccepted ?: false,
        isJudged = judgementType != null,
        isAddingPenalty = judgementType?.isAddingPenalty ?: false,
        result = judgementType?.verdict ?: "",
        problemId = problem.id,
        teamId = teamId,
        percentage = when (problem.testCount) {
            null, 0 -> if (judgementType != null) 1.0 else 0.0
            else -> minOf(passedCaseRun.size.toDouble() / problem.testCount, 1.0)
        },
        time = submissionTime,
        isFirstSolvedRun = false,
    )
}
