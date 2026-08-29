package org.icpclive.data

import kotlinx.coroutines.CoroutineScope
import org.icpclive.Config
import org.icpclive.admin.createUsersController
import org.icpclive.api.*
import org.icpclive.controllers.*
import org.icpclive.util.loadSVG
import org.icpclive.util.toBase64SVG

class Controllers(scope: CoroutineScope): CoroutineScope by scope {
    private val WidgetManager = WidgetManager()
    private val TickerManager = TickerManager()

    private val showOrderCounter = ShowOrderCounter()

    val persistence = PersistenceRegistry()

    val queue = scope.SingleWidgetController(QueueSettings(), WidgetManager, showOrderCounter, ::QueueWidget)
    val statistics = scope.SingleWidgetController(StatisticsSettings(), WidgetManager, showOrderCounter, ::StatisticsWidget)
    val ticker = scope.SingleWidgetController(TickerSettings(), WidgetManager, showOrderCounter, ::TickerWidget)
    val scoreboard = scope.SingleWidgetController(ScoreboardSettings(), WidgetManager, showOrderCounter, ::ScoreboardWidget)
    val fullScreenClock = scope.SingleWidgetController(FullScreenClockSettings(), WidgetManager, showOrderCounter, ::FullScreenClockWidget)
    private val teamViews = TeamViewPosition.entries.associateWith { TeamViewController(WidgetManager, scope, showOrderCounter, it) }
    fun teamView(position: TeamViewPosition): TeamViewController = teamViews[position]!!

    val locator = LocatorWidgetController(WidgetManager, scope, showOrderCounter)

    val advertisement = PresetsController<_, AdvertisementWidget>(WidgetManager, scope, showOrderCounter, ::AdvertisementWidget)
    val picture = PresetsController<_, PictureWidget>(WidgetManager, scope, showOrderCounter, ::PictureWidget)
    val title = PresetsController<_, SvgWidget>(WidgetManager, scope, showOrderCounter) { titleSettings: TitleSettings ->
        SvgWidget(
            loadSVG(Config.mediaDirectory.resolve(titleSettings.preset), titleSettings.data, null).toBase64SVG()
        )
    }
    val tickerMessage = PresetsController(TickerManager, scope, showOrderCounter, TickerMessageSettings::toMessage)
    val userController = Config.createUsersController()

    suspend fun getWidgetStats() = WidgetManager.getUsageStatistics()
}
