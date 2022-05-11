package org.icpclive.cds.wf2.model

import org.icpclive.cds.RunInfo
import kotlin.time.Duration
import kotlin.time.DurationUnit

class WF2RunInfo(
    override val id: Int, override val problemId: Int, override val teamId: Int, private val submissionTime: Duration
) : RunInfo {
    override var result = ""
    override val isJudged:Boolean
        get() = result != ""
    override val isAddingPenalty: Boolean
        get() = "AC" != result && "CE" != result
    override val isAccepted: Boolean
        get() = result == "AC"
    override val percentage = 0.0
    override var lastUpdateTime = submissionTime.toLong(DurationUnit.MILLISECONDS)
    override val time = lastUpdateTime
}
