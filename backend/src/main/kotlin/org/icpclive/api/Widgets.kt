@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

@Serializable
sealed class Widget(
    @SerialName("widgetId") override val id: String, val location: LocationRectangle
) : TypeWithId

@Serializable
class AdvertisementWidget(val advertisement: AdvertisementSettings) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "advertisement"
        val location = LocationRectangle(0, 860, 1920, 90)
    }
}

@Serializable
class PictureWidget(val picture: PictureSettings) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "picture"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
class SvgWidget(val content: String) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "svg"
        val location = LocationRectangle(0, 0, 1920, 1080)
    }
}

@Serializable
class QueueWidget(val settings: QueueSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "queue"
        val location = LocationRectangle(30, 40, 515, 970)
    }
}

@Serializable
class ScoreboardWidget(val settings: ScoreboardSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "scoreboard"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
class StatisticsWidget(val settings: StatisticsSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "statistics"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
class TickerWidget(val settings: TickerSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "ticker"
        val location = LocationRectangle(0, 1025, 1920, 50)
    }
}

@Serializable
enum class TeamViewPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}

@Serializable
class TeamViewWidget(
    val settings: TeamViewSettings, private val position: TeamViewPosition? = null
) : Widget(getWidgetId(position), getLocation(position)) {
    companion object {
        val getWidgetId = { position: TeamViewPosition? ->
            "teamview" + position?.name
        }
        val getLocation = { position: TeamViewPosition? ->
            when (position) {
                TeamViewPosition.TOP_LEFT -> LocationRectangle(30, 40, 915, 475)
                TeamViewPosition.TOP_RIGHT -> LocationRectangle(975, 40, 915, 475)
                TeamViewPosition.BOTTOM_LEFT -> LocationRectangle(30, 535, 915, 475)
                TeamViewPosition.BOTTOM_RIGHT -> LocationRectangle(975, 535, 915, 475)
                else -> LocationRectangle(550, 40, 1350, 970)
            }
        }
    }
}

@Serializable
class TeamPVPWidget(val settings: TeamPVPSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "teampvp"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}
