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
    @SerialName("widgetId") override val id: String,
    val location: LocationRectangle
) : TypeWithId

@Serializable
@SerialName("AdvertisementWidget")
class AdvertisementWidget(val advertisement: AdvertisementSettings) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "advertisement"
        val location = LocationRectangle(0, 860, 1920, 90)
    }
}

@Serializable
@SerialName("PictureWidget")
class PictureWidget(val picture: PictureSettings) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "picture"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
@SerialName("SvgWidget")
class SvgWidget(val content: String) : Widget(generateId(WIDGET_ID_PREFIX), location) {
    companion object {
        const val WIDGET_ID_PREFIX = "svg"
        val location = LocationRectangle(0, 0, 1920, 1080)
    }
}

@Serializable
@SerialName("QueueWidget")
class QueueWidget(val settings: QueueSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "queue"
        val location = LocationRectangle(30, 40, 515, 970)
    }
}

@Serializable
@SerialName("ScoreboardWidget")
class ScoreboardWidget(val settings: ScoreboardSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "scoreboard"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
@SerialName("StatisticsWidget")
class StatisticsWidget(val settings: StatisticsSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "statistics"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}

@Serializable
@SerialName("TickerWidget")
class TickerWidget(val settings: TickerSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "ticker"
        val location = LocationRectangle(0, 1025, 1920, 50)
    }
}

@Serializable
@SerialName("TeamViewWidget")
class TeamViewWidget(val settings: TeamViewSettings) : Widget(WIDGET_ID, location) {
    companion object {
        const val WIDGET_ID = "teamview"
        val location = LocationRectangle(550, 40, 1350, 970)
    }
}
