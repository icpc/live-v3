package org.icpclive.cds.clics.model

import org.icpclive.api.ICPCRunResult
import org.icpclive.api.MediaType
import org.icpclive.api.Verdict
import org.icpclive.cds.clics.api.Problem
import kotlin.time.Duration

class ClicsRunInfo(
    val id: Int,
    val problem: Problem,
    val liveProblemId: Int,
    val teamId: Int,
    val submissionTime: Duration,
    val reactionVideos: List<MediaType> = emptyList(),
) {
    val passedCaseRun = mutableSetOf<Int>()
    var judgementType: ClicsJudgementTypeInfo? = null

    fun toApi() = org.icpclive.api.RunInfo(
        id = id,
        judgementType?.let {
            Verdict.lookup(
                shortName = it.id,
                isAccepted = it.isAccepted,
                isAddingPenalty = it.isAddingPenalty,
            ).toRunResult()
        },
        problemId = liveProblemId,
        teamId = teamId,
        percentage = when (problem.test_data_count) {
            null, 0 -> if (judgementType != null) 1.0 else 0.0
            else -> minOf(passedCaseRun.size.toDouble() / problem.test_data_count, 1.0)
        },
        time = submissionTime,
        reactionVideos = reactionVideos,
    )
}
