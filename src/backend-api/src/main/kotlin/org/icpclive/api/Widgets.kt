@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random
import kotlin.random.nextUInt

fun generateId(widgetPrefix: String): String = "$widgetPrefix-${Random.nextUInt()}"

@Serializable
sealed class Widget(
    @SerialName("widgetId") override val id: String,
    val widgetLocationId: String,
    val statisticsId: String,
) : TypeWithId

@Serializable
@SerialName("AdvertisementWidget")
class AdvertisementWidget(val advertisement: AdvertisementSettings) : Widget(
    id = generateId(WIDGET_ID_PREFIX),
    widgetLocationId = WIDGET_ID_PREFIX,
    statisticsId = WIDGET_ID_PREFIX,
) {
    companion object {
        const val WIDGET_ID_PREFIX = "advertisement"
    }
}

@Serializable
@SerialName("PictureWidget")
class PictureWidget(val picture: PictureSettings) : Widget(
    id = generateId(WIDGET_ID_PREFIX),
    widgetLocationId = WIDGET_ID_PREFIX,
    statisticsId = WIDGET_ID_PREFIX
) {
    companion object {
        const val WIDGET_ID_PREFIX = "picture"
    }
}

@Serializable
@SerialName("SvgWidget")
class SvgWidget(val content: String) : Widget(
    id = generateId(WIDGET_ID_PREFIX),
    widgetLocationId = WIDGET_ID_PREFIX,
    statisticsId = WIDGET_ID_PREFIX
) {
    companion object {
        const val WIDGET_ID_PREFIX = "svg"
    }
}

@Serializable
@SerialName("QueueWidget")
class QueueWidget(val settings: QueueSettings) : Widget(
    id = WIDGET_ID,
    widgetLocationId = WIDGET_ID,
    statisticsId = WIDGET_ID,
) {
    companion object {
        const val WIDGET_ID = "queue"
    }
}

@Serializable
@SerialName("ScoreboardWidget")
class ScoreboardWidget(val settings: ScoreboardSettings) : Widget(
    id = WIDGET_ID,
    widgetLocationId = WIDGET_ID,
    statisticsId = WIDGET_ID,
) {
    companion object {
        const val WIDGET_ID = "scoreboard"
    }
}

@Serializable
@SerialName("StatisticsWidget")
class StatisticsWidget(val settings: StatisticsSettings) : Widget(
    id = WIDGET_ID,
    widgetLocationId = WIDGET_ID,
    statisticsId = WIDGET_ID
) {
    companion object {
        const val WIDGET_ID = "statistics"
    }
}

@Serializable
@SerialName("TickerWidget")
class TickerWidget(val settings: TickerSettings) : Widget(
    id = WIDGET_ID,
    widgetLocationId = WIDGET_ID,
    statisticsId = WIDGET_ID
) {
    companion object {
        const val WIDGET_ID = "ticker"
    }
}

@Serializable
enum class TeamViewPosition {
    SINGLE, PVP_TOP, PVP_BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}


@Serializable
@SerialName("TeamViewWidget")
class TeamViewWidget(
    val settings: OverlayTeamViewSettings
) : Widget(
    getWidgetId(settings.position),
    getWidgetId(settings.position),
    WIDGET_ID_PREFIX
) {
    companion object {
        private const val WIDGET_ID_PREFIX = "teamview"
        fun getWidgetId(position: TeamViewPosition) = "$WIDGET_ID_PREFIX.${position.name}"
    }
}

@Serializable
@SerialName("FullScreenClockWidget")
class FullScreenClockWidget(val settings: FullScreenClockSettings) : Widget(
    WIDGET_ID,
    WIDGET_ID,
    WIDGET_ID
) {
    companion object {
        const val WIDGET_ID = "fullScreenClock"
    }
}

@Serializable
@SerialName("TeamLocatorWidget")
class TeamLocatorWidget(val settings: TeamLocatorSettings) : Widget(
    WIDGET_ID,
    WIDGET_ID,
    WIDGET_ID,
) {
    companion object {
        const val WIDGET_ID = "teamLocator"
    }
}
