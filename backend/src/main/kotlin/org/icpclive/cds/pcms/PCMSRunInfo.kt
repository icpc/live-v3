package org.icpclive.cds.pcms

import org.icpclive.cds.RunInfo

class PCMSRunInfo(
    override val id: Int,
    override var isJudged: Boolean,
    override var result: String,
    override val problemId: Int,
    override val time: Long,
    override val teamId: Int,
    override val lastUpdateTime: Long,
) : RunInfo {
    override val isAccepted: Boolean
        get() = "AC" == result
    override val isAddingPenalty: Boolean
        get() = !isAccepted && "CE" != result
    override val percentage: Double
        get() = 0.0
    override var isFirstSolvedRun = false
}