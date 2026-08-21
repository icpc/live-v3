package org.icpclive.data

import kotlinx.coroutines.CoroutineScope

import org.icpclive.Config
import org.icpclive.admin.createUsersController
import org.icpclive.api.*
import org.icpclive.controllers.*
import org.icpclive.util.loadSVG
import org.icpclive.util.toBase64SVG

class Controllers(scope: CoroutineScope) : CoroutineScope by scope {
    private val WidgetManager = WidgetManager()
    private val TickerManager = TickerManager()
    val queue = scope.SingleWidgetController(QueueSettings(), WidgetManager, ::QueueWidget)
    val statistics = scope.SingleWidgetController(StatisticsSettings(), WidgetManager, ::StatisticsWidget)
    val ticker = scope.SingleWidgetController(TickerSettings(), WidgetManager, ::TickerWidget)
    val scoreboard = scope.SingleWidgetController(ScoreboardSettings(), WidgetManager, ::ScoreboardWidget)
    val fullScreenClock = scope.SingleWidgetController(FullScreenClockSettings(), WidgetManager, ::FullScreenClockWidget)
    private val teamViews = TeamViewPosition.entries.associateWith { TeamViewController(WidgetManager, scope, it) }
    fun teamView(position: TeamViewPosition): TeamViewController = teamViews[position]!!

    val locator = LocatorWidgetController(WidgetManager, scope)

    private fun presetsPath(name: String) = Config.presetsDirectory.resolve("$name.json")

    val advertisement = PresetsController<_, AdvertisementWidget>(presetsPath("advertisements"), WidgetManager, scope, ::AdvertisementWidget)
    val picture = PresetsController<_, PictureWidget>(presetsPath("pictures"), WidgetManager, scope, ::PictureWidget)
    val title = PresetsController<_, SvgWidget>(presetsPath("title"), WidgetManager, scope) { titleSettings: TitleSettings ->
        SvgWidget(
            loadSVG(Config.mediaDirectory.resolve(titleSettings.preset), titleSettings.data, null).toBase64SVG()
        )
    }
    val tickerMessage = PresetsController(presetsPath("ticker"), TickerManager, scope, TickerMessageSettings::toMessage)
    val userController = Config.createUsersController()

    suspend fun getWidgetStats() = WidgetManager.getUsageStatistics()
}
