@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.events.RunInfo as EventsRunInfo

@Serializable
data class Advertisement(val text: String)

@Serializable
data class Picture(val url: String, val name: String)

@Serializable
class QueueSettings() // TODO??

@Serializable
data class RunInfo(
    val id:Int,
    val isAccepted:Boolean,
    val isJudged:Boolean,
    val result:String,
    val problemId:Int,
    val teamId: Int,
    val isReallyUnknown: Boolean,
    val percentage: Double,
    val time: Long,
    val lastUpdateTime: Long,
    val isFirstSolvedRun: Boolean,
) {
    constructor(info: EventsRunInfo): this(
        info.id,
        info.isAccepted,
        info.isJudged,
        info.result,
        info.problemId,
        info.teamId,
        info.isReallyUnknown,
        info.percentage,
        info.time,
        info.lastUpdateTime,
        info.isFirstSolvedRun
    )
}

fun EventsRunInfo.toApi() = RunInfo(this)