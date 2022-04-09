package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ObjectSettings

@Serializable
data class AdvertisementSettings(val text: String) : ObjectSettings

@Serializable
data class PictureSettings(val url: String, val name: String) : ObjectSettings

@Serializable
class QueueSettings : ObjectSettings

@Serializable
class ScoreboardSettings : ObjectSettings

@Serializable
class StatisticsSettings : ObjectSettings

@Serializable
class TickerSettings : ObjectSettings

@Serializable
data class TeamViewSettings(val teamId: Int = 0, val mediaType: MediaType? = null) : ObjectSettings

@Serializable
sealed class TickerMessageSettings : ObjectSettings {
    abstract val part: TickerPart
    abstract val periodMs: Long
    abstract fun toMessage(): TickerMessage
}

@Serializable
enum class TickerPart {
    short,
    long;
}

@Serializable
@SerialName("text")
data class TextTickerSettings(
    override val part: TickerPart,
    override val periodMs: Long,
    val text: String
) : TickerMessageSettings() {
    override fun toMessage() = TextTickerMessage(this)
}

@Serializable
@SerialName("clock")
data class ClockTickerSettings(override val part: TickerPart, override val periodMs: Long) : TickerMessageSettings() {
    override fun toMessage() = ClockTickerMessage(this)
}

@Serializable
@SerialName("scoreboard")
data class ScoreboardTickerSettings(
    override val part: TickerPart,
    override val periodMs: Long,
    val from: Int,
    val to: Int
) : TickerMessageSettings() {
    override fun toMessage() = ScoreboardTickerMessage(this)
}
