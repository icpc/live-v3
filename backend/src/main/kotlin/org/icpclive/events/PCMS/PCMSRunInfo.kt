package org.icpclive.events.PCMS

import org.icpclive.events.EventsLoader
import org.icpclive.events.RunInfo

class PCMSRunInfo(
    override var isJudged: Boolean,
    override var result: String,
    override val problemId: Int,
    override val time: Long,
    val timestamp: Long,
    override val teamId: Int
    ) : RunInfo {

    constructor(run: PCMSRunInfo): this(
        run.isJudged, run.result, run.problemId, run.time, run.timestamp, run.teamId
    ) {
        lastUpdateTime = run.lastUpdateTime
    }

    override val isAccepted: Boolean
        get() = "AC" == result
    override val isAddingPenalty: Boolean
        get() = isJudged && !isAccepted && "CE" != result

    override val problem
        get() = EventsLoader.instance.contestData!!.problems[problemId]

    override val percentage: Double
        get() = 0.0
    override var id = 0
    override var lastUpdateTime: Long = 0
        set(value) {
            field = System.currentTimeMillis()
        }
    override var isReallyUnknown = false
}