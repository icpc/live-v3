@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextUInt

var defaultWidgetPositions: Map<String, LocationRectangle> = emptyMap()

fun generateId(widgetPrefix: String): String = "$widgetPrefix-${Random.nextUInt()}"

@Serializable
class LocationRectangle(
    val positionX: Int,
    val positionY: Int,
    val sizeX: Int,
    val sizeY: Int,
)

fun getLocationOrDefault(widgetPrefix: String, defaultLocationRectangle: LocationRectangle) =
    defaultWidgetPositions.getOrDefault(widgetPrefix, defaultLocationRectangle)

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
        val location = LocationRectangle(16, 16, 1488, 984)
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
        val location = LocationRectangle(16, 16, 1488, 984)
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
        val location = LocationRectangle(1520, 248, 384, 752)
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
        val location = LocationRectangle(16, 16, 1488, 984)
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
        val location = LocationRectangle(16, 662, 1488, 338)
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
        val location = LocationRectangle(16, 1016, 1888, 48)
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
            TeamViewPosition.SINGLE_TOP_RIGHT -> LocationRectangle(16, 16, 1488, 984)
            TeamViewPosition.PVP_TOP -> LocationRectangle(16, 16, 1488, 984 / 2 + 16)
            TeamViewPosition.PVP_BOTTOM -> LocationRectangle(16, 16 + 984 / 2 - 16, 1488, 984 / 2 + 16)
            TeamViewPosition.TOP_LEFT -> LocationRectangle(16, 16, 1488 / 2, 837 / 2)
            TeamViewPosition.TOP_RIGHT -> LocationRectangle(16 + 1488 / 2, 16, 1488 / 2, 837 / 2)
            TeamViewPosition.BOTTOM_LEFT -> LocationRectangle(16, 16 + 837 / 2, 1488 / 2, 837 / 2)
            TeamViewPosition.BOTTOM_RIGHT -> LocationRectangle(16 + 1488 / 2, 16 + 837 / 2, 1488 / 2, 837 / 2)
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
        val location = LocationRectangle(16, 16, 1488, 984)
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
