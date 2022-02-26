package org.icpclive.cds

interface RunInfo {
    val id: Int
    val isAccepted: Boolean
    val isJudged: Boolean
    val isAddingPenalty: Boolean
    val result: String
    val problemId: Int
    val teamId: Int
    val percentage: Double
    val time: Long
    val lastUpdateTime: Long

    fun toApi() = org.icpclive.api.RunInfo(
        id,
        isAccepted,
        isJudged,
        isAddingPenalty,
        result,
        problemId,
        teamId,
        percentage,
        time,
        false,
    )
}