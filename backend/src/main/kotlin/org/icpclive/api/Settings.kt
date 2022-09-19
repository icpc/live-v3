@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface ObjectSettings

@Serializable
data class AdvertisementSettings(val text: String) : ObjectSettings

@Serializable
data class TitleSettings(val preset: String, val data: Map<String, String>) : ObjectSettings

@Serializable
data class PictureSettings(val url: String, val name: String) : ObjectSettings

@Serializable
class QueueSettings : ObjectSettings

@Serializable
enum class OptimismLevel {
    @SerialName("normal")
    NORMAL,

    @SerialName("optimistic")
    OPTIMISTIC,

    @SerialName("pessimistic")
    PESSIMISTIC;
}

@Serializable
data class ScoreboardSettings(
    val isInfinite: Boolean = true,
    val numPages: Int? = null,
    val startFromPage: Int = 1,
    val optimismLevel: OptimismLevel = OptimismLevel.NORMAL,
    val teamsOnPage: Int = 23,
    val group: String = "all"
) : ObjectSettings

@Serializable
class StatisticsSettings : ObjectSettings

@Serializable
class TickerSettings : ObjectSettings

@Serializable
data class TeamViewSettings(
    val teamId: Int? = null,
    val mediaType: MediaType? = null,
    val showTaskStatus: Boolean = true,
    val showAchievement: Boolean = false,
    val position: TeamViewPosition = TeamViewPosition.SINGLE_TOP_RIGHT,
) : ObjectSettings

@Serializable
data class TeamPVPSettings(
    val teamId: List<Int> = emptyList(), val mediaType: List<MediaType> = emptyList()
) : ObjectSettings

@Serializable
data class TeamLocatorSettings(
    val x: Int = 1920 / 2,
    val y: Int = 1080 / 2,
    val radius: Int = 50,
) : ObjectSettings

@Serializable
sealed class TickerMessageSettings : ObjectSettings {
    abstract val part: TickerPart
    abstract val periodMs: Long
    abstract fun toMessage(): TickerMessage
}

@Serializable
enum class TickerPart {
    @SerialName("short")
    SHORT,

    @SerialName("long")
    LONG;
}

@Serializable
@SerialName("text")
data class TextTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val text: String
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
    override val part: TickerPart, override val periodMs: Long, val from: Int, val to: Int
) : TickerMessageSettings() {
    override fun toMessage() = ScoreboardTickerMessage(this)
}
