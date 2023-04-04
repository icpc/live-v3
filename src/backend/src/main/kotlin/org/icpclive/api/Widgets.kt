@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.config
import kotlin.random.Random
import kotlin.random.nextUInt

fun generateId(widgetPrefix: String): String = "$widgetPrefix-${Random.nextUInt()}"

@Serializable
class LocationRectangle(
    val positionX: Int,
    val positionY: Int,
    val sizeX: Int,
    val sizeY: Int,
)

fun getLocationOrDefault(widgetPrefix: String, defaultLocationRectangle: LocationRectangle) =
    config.widgetPositions.getOrDefault(widgetPrefix, defaultLocationRectangle)

@Serializable
sealed class Widget(
    @SerialName("widgetId") override val id: String,
    val location: LocationRectangle
) : TypeWithId

@Serializable
@SerialName("AdvertisementWidget")
class AdvertisementWidget(val advertisement: AdvertisementSettings) : Widget(
    generateId(WIDGET_ID_PREFIX),
    getLocationOrDefault(WIDGET_ID_PREFIX, location)
) {
    companion object {
        const val WIDGET_ID_PREFIX = "advertisement"
        val location = LocationRectangle(0, 860, 1920, 90)
    }
}

@Serializable
@SerialName("PictureWidget")
class PictureWidget(val picture: PictureSettings) : Widget(
    generateId(WIDGET_ID_PREFIX),
    getLocationOrDefault(WIDGET_ID_PREFIX, location)
) {
    companion object {
        const val WIDGET_ID_PREFIX = "picture"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
@SerialName("SvgWidget")
class SvgWidget(val content: String) : Widget(
    generateId(WIDGET_ID_PREFIX),
    getLocationOrDefault(WIDGET_ID_PREFIX, location)
) {
    companion object {
        const val WIDGET_ID_PREFIX = "svg"
        val location = LocationRectangle(0, 0, 1920, 1080)
    }
}

@Serializable
@SerialName("QueueWidget")
class QueueWidget(val settings: QueueSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location)
) {
    companion object {
        const val WIDGET_ID = "queue"
        val location = LocationRectangle(20, 310, 515, 695)
    }
}

@Serializable
@SerialName("ScoreboardWidget")
class ScoreboardWidget(val settings: ScoreboardSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location)
) {
    companion object {
        const val WIDGET_ID = "scoreboard"
        val location = LocationRectangle(550, 20, 1350, 985)
    }
}

@Serializable
@SerialName("StatisticsWidget")
class StatisticsWidget(val settings: StatisticsSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location)
) {
    companion object {
        const val WIDGET_ID = "statistics"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
@SerialName("TickerWidget")
class TickerWidget(val settings: TickerSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location)
) {
    companion object {
        const val WIDGET_ID = "ticker"
        val location = LocationRectangle(0, 1025, 1920, 50)
    }
}

@Serializable
enum class TeamViewPosition {
    SINGLE_TOP_RIGHT, PVP_TOP, PVP_BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Serializable
@SerialName("TeamViewWidget")
class TeamViewWidget(
    val settings: OverlayTeamViewSettings
) : Widget(
    getWidgetId(settings.position),
    getLocationOrDefault(getWidgetId(settings.position), getLocation(settings.position))
) {
    companion object {
        fun getWidgetId(position: TeamViewPosition) = "teamview." + position.name
        fun getLocation(position: TeamViewPosition) = when (position) {
            TeamViewPosition.SINGLE_TOP_RIGHT -> LocationRectangle(550, 40, 1350, 970)
            TeamViewPosition.PVP_TOP -> LocationRectangle(550, 40, 1350, 970 / 2)
            TeamViewPosition.PVP_BOTTOM -> LocationRectangle(550, 40 + 970 / 2, 1350, 970 / 2)
            TeamViewPosition.TOP_LEFT -> LocationRectangle(550, 40, 672, 378)
            TeamViewPosition.TOP_RIGHT -> LocationRectangle(550 + 672, 40, 672, 378)
            TeamViewPosition.BOTTOM_LEFT -> LocationRectangle(550, 40 + 378, 672, 378)
            TeamViewPosition.BOTTOM_RIGHT -> LocationRectangle(550 + 672, 40 + 378, 672, 378)
        }
    }
}

@Serializable
@SerialName("FullScreenClockWidget")
class FullScreenClockWidget(val settings: FullScreenClockSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location)
) {
    companion object {
        const val WIDGET_ID = "fullScreenClock"
        val location = LocationRectangle(0, 0, 1920, 1080)
    }
}

@Serializable
@SerialName("TeamLocatorWidget")
class TeamLocatorWidget(val settings: TeamLocatorSettings) : Widget(
    WIDGET_ID,
    getLocationOrDefault(WIDGET_ID, location),
) {
    companion object {
        const val WIDGET_ID = "teamLocator"
        val location = LocationRectangle(0, 0, 1920, 1080)
    }
}
