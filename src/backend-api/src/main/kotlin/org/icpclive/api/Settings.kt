@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

interface ObjectSettings

object UnitSettings : ObjectSettings

@Serializable
data class AdvertisementSettings(val text: String) : ObjectSettings

@Serializable
data class TitleSettings(val preset: String, val data: Map<String, String>) : ObjectSettings

@Serializable
data class PictureSettings(val url: String, val name: String) : ObjectSettings

@Serializable
class QueueSettings : ObjectSettings

@Serializable
data class ScoreboardSettings(
    val isInfinite: Boolean = true,
    val numRows: Int? = null,
    val startFromRow: Int = 1,
    val optimismLevel: OptimismLevel = OptimismLevel.NORMAL,
    val group: String = "all"
) : ObjectSettings

@Serializable
class StatisticsSettings : ObjectSettings

@Serializable
class TickerSettings : ObjectSettings

@Serializable
data class ExternalTeamViewSettings(
    val teamId: TeamId? = null,
    val mediaTypes: List<TeamMediaType> = emptyList(),
    val showTaskStatus: Boolean = true,
    val showAchievement: Boolean = false,
    val position: TeamViewPosition = TeamViewPosition.SINGLE_TOP_RIGHT,
) : ObjectSettings

@Serializable
data class OverlayTeamViewSettings(
    val content: List<MediaType> = emptyList(),
    val position: TeamViewPosition = TeamViewPosition.SINGLE_TOP_RIGHT,
) : ObjectSettings

@Serializable
class FullScreenClockSettings(
    val globalTimeMode: Boolean = false,
    val quietMode: Boolean = false,
    val contestCountdownMode: Boolean = false,
) : ObjectSettings

@Serializable
data class TeamLocatorCircleSettings(
    val x: Int,
    val y: Int,
    val radius: Int,
    val teamId: TeamId,
)

@Serializable
data class TeamLocatorSettings(
    val circles: List<TeamLocatorCircleSettings> = emptyList(),
    val scene: String = "default", // FIXME: feature for multi vmix sources coordination. Should be moved to the Widget class
) : ObjectSettings

@Serializable
data class TeamLocatorExternalCircleSettings(
    val x: Int,
    val y: Int,
    val radius: Int,
    val teamId: TeamId? = null,
    val cdsTeamId: String? = null,
)

@Serializable
data class ExternalTeamLocatorSettings(
    val circles: List<TeamLocatorExternalCircleSettings> = emptyList(),
    val scene: String = "default"
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
@SerialName("image")
data class ImageTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val path: String
) : TickerMessageSettings() {
    override fun toMessage() = ImageTickerMessage(this)
}

@Serializable
@SerialName("clock")
data class ClockTickerSettings(
    override val part: TickerPart,
    override val periodMs: Long,
    val timeZone: String? = null
) : TickerMessageSettings() {
    override fun toMessage(): ClockTickerMessage {
        if (timeZone != null && timeZone.isEmpty()) {
            return ClockTickerMessage(ClockTickerSettings(part, periodMs, null))
        }
        return ClockTickerMessage(this)
    }
}

@Serializable
@SerialName("scoreboard")
data class ScoreboardTickerSettings(
    override val part: TickerPart, override val periodMs: Long, val from: Int, val to: Int
) : TickerMessageSettings() {
    override fun toMessage() = ScoreboardTickerMessage(this)
}
