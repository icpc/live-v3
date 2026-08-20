package org.icpclive.data

import org.icpclive.Config
import org.icpclive.admin.createUsersController
import org.icpclive.api.*
import org.icpclive.controllers.*
import org.icpclive.util.loadSVG
import org.icpclive.util.toBase64SVG

object Controllers {
    private val WidgetManager = WidgetManager()
    private val TickerManager = TickerManager()

    private fun presetsPath(name: String) = Config.presetsDirectory.resolve("$name.json")

    private val widgetsState = WidgetsStateController(presetsPath("widgets"))

    val queue = widgetsState.register("queue", SingleWidgetController(QueueSettings(), WidgetManager, ::QueueWidget))
    val statistics =
        widgetsState.register("statistics", SingleWidgetController(StatisticsSettings(), WidgetManager, ::StatisticsWidget))
    val ticker = widgetsState.register("ticker", SingleWidgetController(TickerSettings(), WidgetManager, ::TickerWidget))
    val scoreboard =
        widgetsState.register("scoreboard", SingleWidgetController(ScoreboardSettings(), WidgetManager, ::ScoreboardWidget))
    val fullScreenClock = widgetsState.register(
        "fullScreenClock",
        SingleWidgetController(FullScreenClockSettings(), WidgetManager, ::FullScreenClockWidget)
    )
    private val teamViews = TeamViewPosition.entries.associateWith { TeamViewController(WidgetManager, it) }
    fun teamView(position: TeamViewPosition): TeamViewController = teamViews[position]!!

    val locator = LocatorWidgetController(WidgetManager)

    val advertisement = PresetsController(presetsPath("advertisements"), WidgetManager, ::AdvertisementWidget)
    val picture = PresetsController(presetsPath("pictures"), WidgetManager, ::PictureWidget)
    val title = PresetsController(presetsPath("title"), WidgetManager) { titleSettings: TitleSettings ->
        SvgWidget(
            loadSVG(Config.mediaDirectory.resolve(titleSettings.preset), titleSettings.data, null).toBase64SVG()
        )
    }
    val tickerMessage = PresetsController(presetsPath("ticker"), TickerManager, TickerMessageSettings::toMessage)
    val userController = Config.createUsersController()

    init {
        widgetsState.launchStateSync()
    }

    suspend fun getWidgetStats() = WidgetManager.getUsageStatistics()
}
