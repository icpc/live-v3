@file:Suppress("UNUSED")
package org.icpclive.api


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CreepingLinePart {
    short,
    long;
}

@Serializable
sealed class CreepingLineMessage {
    abstract val id: Long
    abstract val part: CreepingLinePart
    abstract val periodMs: Long
}

@Serializable
@SerialName("text")
data class TextCreepingLineMessage(
    val text: String,
    override val id: Long,
    override val part: CreepingLinePart,
    override val periodMs: Long
) : CreepingLineMessage()

@Serializable
@SerialName("clock")
data class ClockCreepingLineMessage(
    override val id: Long,
    override val part: CreepingLinePart,
    override val periodMs: Long
) : CreepingLineMessage()

@Serializable
@SerialName("scoreboard")
data class ScoreboardCreepingLineMessage(
    val from:Int,
    val to:Int,
    override val id: Long,
    override val part: CreepingLinePart,
    override val periodMs: Long
) : CreepingLineMessage()