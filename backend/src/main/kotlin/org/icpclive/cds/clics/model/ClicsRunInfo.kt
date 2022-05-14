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
    override var result = ""
    override val isJudged: Boolean
        get() = result != ""
    override val isAddingPenalty: Boolean
        get() = "AC" != result && "CE" != result
    override val isAccepted: Boolean
        get() = result == "AC"
    override val percentage: Double
        get() = if (problem.testCount == null || problem.testCount == 0) {
            0.0
        } else {
            minOf(1.0 * passedCaseRun.size / problem.testCount, 1.0)
        }
}
