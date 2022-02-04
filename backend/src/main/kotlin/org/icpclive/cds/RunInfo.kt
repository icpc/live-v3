package org.icpclive.cds

interface RunInfo {
    val id: Int
    val isAccepted: Boolean
    val isAddingPenalty: Boolean
    val isJudged: Boolean
    val result: String
    val problemId: Int
    val teamId: Int
    val percentage: Double
    val time: Long
    val lastUpdateTime: Long
    val isFirstSolvedRun: Boolean

    fun toApi() = org.icpclive.api.RunInfo(
        id,
        isAccepted,
        isJudged,
        result,
        problemId,
        teamId,
        percentage,
        time,
        lastUpdateTime,
        isFirstSolvedRun
    )
}