package org.icpclive.cds.pcms

import org.icpclive.cds.RunInfo

class PCMSRunInfo(
    override val id: Int,
    override val isJudged: Boolean,
    override val result: String,
    override val problemId: Int,
    override val time: Long,
    override val teamId: Int,
    override val percentage: Double,
    override val lastUpdateTime: Long
) : RunInfo {
    override val isAccepted: Boolean
        get() = "AC" == result
    override val isAddingPenalty: Boolean
        get() = !isAccepted && "CE" != result
}