@file:Suppress("UNUSED")

package org.icpclive.api


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
sealed class TickerMessage (
    override val id: String
) : TypeWithId

@Serializable
@SerialName("text")
class TextTickerMessage(val settings: TextTickerSettings) : TickerMessage(ID) {
    companion object {
        val ID: String = Random.nextInt().toString()
    }
}

@Serializable
@SerialName("clock")
class ClockTickerMessage(val settings: ClockTickerSettings) : TickerMessage(ID) {
    companion object {
        val ID: String = Random.nextInt().toString()
    }
}

@Serializable
@SerialName("scoreboard")
class ScoreboardTickerMessage(val settings: ScoreboardTickerSettings) : TickerMessage(ID) {
    companion object {
        val ID: String = Random.nextInt().toString()
    }
}