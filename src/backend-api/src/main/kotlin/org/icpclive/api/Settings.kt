@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import kotlin.time.Duration

interface ObjectSettings

object UnitSettings : ObjectSettings

@Serializable
data class AdvertisementSettings(val text: String) : ObjectSettings

@Serializable
data class TitleSettings(
    val preset: String,
    val leftPreset: String? = null,
    val rightPreset: String? = null,
    val data: Map<String, String>) : ObjectSettings

@Serializable
data class PictureSettings(val url: String, val name: String) : ObjectSettings

@Serializable
data class QueueSettings(val horizontal: Boolean = false) : ObjectSettings

@Serializable
data class ScoreboardSettings(
    val scrollDirection: ScoreboardScrollDirection = ScoreboardScrollDirection.Forward,
    val optimismLevel: OptimismLevel = OptimismLevel.NORMAL,
    val group: String = "all"
) : ObjectSettings

enum class ScoreboardScrollDirection{
    FirstPage, Back, Pause, Forward, LastPage
}

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
    val showTimeLine: Boolean = false,
    val position: TeamViewPosition = TeamViewPosition.SINGLE,
) : ObjectSettings

@Serializable
data class OverlayTeamViewSettings(
    val teamId: TeamId,
    val primary: List<MediaType>,
    val secondary: List<MediaType>,
    val showTaskStatus: Boolean,
    val achievement: List<MediaType>,
    val showTimeLine: Boolean,
    val position: TeamViewPosition,
) : ObjectSettings

@Serializable
enum class ClockType {
    @SerialName("standard")
    STANDARD,
    
    @SerialName("countdown")
    COUNTDOWN,
    
    @SerialName("global")
    GLOBAL
}

@Serializable
data class FullScreenClockSettings(
    val clockType: ClockType = ClockType.STANDARD,
    val showSeconds: Boolean = true,
    val timeZone: String? = null,
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
)

@Serializable
data class ExternalTeamLocatorSettings(
    val circles: List<TeamLocatorExternalCircleSettings> = emptyList(),
    val scene: String = "default"
) : ObjectSettings


@Serializable
sealed class TickerMessageSettings : ObjectSettings {
    abstract val part: TickerPart
    abstract val period: Duration
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
    override val part: TickerPart,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("periodMs")
    override val period: Duration,
    val text: String
) : TickerMessageSettings() {
    override fun toMessage() = TickerMessage(this)
}

@Serializable
@SerialName("image")
data class ImageTickerSettings(
    override val part: TickerPart,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("periodMs")
    override val period: Duration,
    val path: String
) : TickerMessageSettings() {
    override fun toMessage() = TickerMessage(this)
}

@Serializable
@SerialName("clock")
data class ClockTickerSettings(
    override val part: TickerPart,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("periodMs")
    override val period: Duration,
    val clockType: ClockType = ClockType.STANDARD,
    val showSeconds: Boolean = true,
    val timeZone: String? = null,
) : TickerMessageSettings() {
    override fun toMessage(): TickerMessage {
        return TickerMessage(copy(timeZone = timeZone?.takeUnless { it.isEmpty() || it.isBlank() }))
    }
}

@Serializable
@SerialName("scoreboard")
data class ScoreboardTickerSettings(
    override val part: TickerPart,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("periodMs")
    override val period: Duration,
    val from: Int,
    val to: Int
) : TickerMessageSettings() {
    override fun toMessage() = TickerMessage(this)
}

@Serializable
@SerialName("empty")
data class EmptyTickerSettings(
    override val part: TickerPart,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    @SerialName("periodMs")
    override val period: Duration,
) : TickerMessageSettings() {
    override fun toMessage() = TickerMessage(this)
}
