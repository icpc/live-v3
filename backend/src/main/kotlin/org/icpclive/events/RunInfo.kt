package org.icpclive.events

interface RunInfo : Comparable<RunInfo> {
    val id: Int
    val isAccepted: Boolean
    val isAddingPenalty: Boolean
    val isJudged: Boolean
    val result: String
    val problem: ProblemInfo?
    val problemId: Int
    val teamId: Int
    val isReallyUnknown: Boolean
    val percentage: Double
    val time: Long
    val lastUpdateTime: Long
    val isFirstSolvedRun: Boolean
        get() = EventsLoader.instance.contestData!!.firstSolvedRun[problemId] === this

    override fun compareTo(runInfo: RunInfo): Int {
        return time.compareTo(runInfo.time)
    }
}