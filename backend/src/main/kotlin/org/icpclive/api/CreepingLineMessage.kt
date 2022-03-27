@file:Suppress("UNUSED")
package org.icpclive.api


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class TickerPart {
    short,
    long;
}

@Serializable
sealed class TickerMessage {
    abstract val id: Long
    abstract val part: TickerPart
    abstract val periodMs: Long
}

@Serializable
@SerialName("text")
data class TextTickerMessage(
    val text: String,
    override val id: Long,
    override val part: TickerPart,
    override val periodMs: Long
) : TickerMessage()

@Serializable
@SerialName("clock")
data class ClockTickerMessage(
    override val id: Long,
    override val part: TickerPart,
    override val periodMs: Long
) : TickerMessage()

@Serializable
@SerialName("scoreboard")
data class ScoreboardTickerMessage(
    val from:Int,
    val to:Int,
    override val id: Long,
    override val part: TickerPart,
    override val periodMs: Long
) : TickerMessage()
