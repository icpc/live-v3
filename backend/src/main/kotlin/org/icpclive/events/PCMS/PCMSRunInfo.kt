package org.icpclive.events.PCMS

import org.icpclive.events.EventsLoader
import org.icpclive.events.ProblemInfo
import org.icpclive.events.RunInfo

class PCMSRunInfo : RunInfo {
    constructor() {
        isJudged = true
    }

    constructor(judged: Boolean, result: String, problem: Int, time: Long, timestamp: Long, teamId: Int) {
        isJudged = judged
        this.result = result
        problemId = problem
        this.time = time
        this.timestamp = timestamp
        lastUpdateTime = time
        this.teamId = teamId
    }

    constructor(run: RunInfo) {
        isJudged = run.isJudged
        result = run.result
        problemId = run.problemId
        time = run.time
        lastUpdateTime = run.lastUpdateTime
        teamId = run.teamId
    }

    override val isAccepted: Boolean
        get() = "AC" == result
    override val isAddingPenalty: Boolean
        get() = isJudged && !isAccepted && "CE" != result

    override val problem
        get() = EventsLoader.instance.contestData!!.problems[problemId]

    override val percentage: Double
        get() = 0.0
    override var isJudged: Boolean
    override var result = ""
    override var id = 0
    override var teamId = 0
    override var problemId = 0
    override var time: Long = 0
    protected var timestamp: Long = 0
    override var lastUpdateTime: Long = 0
        set(value) {
            field = System.currentTimeMillis()
        }
    override var isReallyUnknown = false
}