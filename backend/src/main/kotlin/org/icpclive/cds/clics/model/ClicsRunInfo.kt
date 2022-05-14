package org.icpclive.cds.clics.model

import org.icpclive.cds.RunInfo
import kotlin.time.Duration
import kotlin.time.DurationUnit

class ClicsRunInfo(
    override val id: Int,
    private val problem: ClicsProblemInfo,
    override val teamId: Int,
    submissionTime: Duration
) : RunInfo {
    override val problemId: Int = problem.id
    override val time = submissionTime.toLong(DurationUnit.MILLISECONDS)
    override var lastUpdateTime = time
    val passedCaseRun = mutableSetOf<Int>()
    var judgementType: ClicsJudgementTypeInfo? = null
    override val result: String
        get() = judgementType?.verdict ?: ""
    override val isJudged: Boolean
        get() = judgementType != null
    override val isAccepted: Boolean
        get() = judgementType?.isAccepted ?: false
    override val isAddingPenalty: Boolean
        get() = judgementType?.isAddingPenalty ?: false
    override val percentage: Double
        get() = if (problem.testCount == null || problem.testCount == 0) {
            0.0
        } else {
            minOf(1.0 * passedCaseRun.size / problem.testCount, 1.0)
        }
}
