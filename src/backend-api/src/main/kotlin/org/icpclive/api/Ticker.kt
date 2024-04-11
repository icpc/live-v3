@file:Suppress("UNUSED")

package org.icpclive.api


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TickerMessage(
    override val id: String,
    val part: TickerPart,
    val periodMs: Long
) : TypeWithId

@Serializable
@SerialName("text")
class TextTickerMessage(val settings: TextTickerSettings) :
    TickerMessage(generateId(TICKER_ID_PREFIX), settings.part, settings.periodMs) {
    companion object {
        const val TICKER_ID_PREFIX: String = "ticker_text"
    }
}

@Serializable
@SerialName("image")
class ImageTickerMessage(val settings: ImageTickerSettings)
    : TickerMessage(generateId(TICKER_ID_PREFIX), settings.part, settings.periodMs) {
        companion object {
            const val TICKER_ID_PREFIX: String = "ticker_image"
        }
    }

@Serializable
@SerialName("clock")
class ClockTickerMessage(val settings: ClockTickerSettings) :
    TickerMessage(generateId(TICKER_ID_PREFIX), settings.part, settings.periodMs) {
    companion object {
        const val TICKER_ID_PREFIX: String = "ticker_clock"
    }
}

@Serializable
@SerialName("scoreboard")
class ScoreboardTickerMessage(val settings: ScoreboardTickerSettings) :
    TickerMessage(generateId(TICKER_ID_PREFIX), settings.part, settings.periodMs) {
    companion object {
        const val TICKER_ID_PREFIX: String = "ticker_scoreboard"
    }
}

@Serializable
@SerialName("empty")
class EmptyTickerMessage(val settings: EmptyTickerSettings) :
    TickerMessage(generateId(TICKER_ID_PREFIX), settings.part, settings.periodMs) {
    companion object {
        const val TICKER_ID_PREFIX: String = "ticker_empty"
    }
}
