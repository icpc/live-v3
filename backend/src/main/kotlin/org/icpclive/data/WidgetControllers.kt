package org.icpclive.data

import org.icpclive.api.*
import org.icpclive.config
import org.icpclive.utils.Svg
import org.icpclive.controllers.*

object WidgetControllers {
    private val WidgetManager = WidgetManager()
    private val TickerManager = TickerManager()
    val queue = SingleWidgetController(QueueSettings(), WidgetManager, ::QueueWidget)
    val statistics = SingleWidgetController(StatisticsSettings(), WidgetManager, ::StatisticsWidget)
    val ticker = SingleWidgetController(TickerSettings(), WidgetManager, ::TickerWidget)
    val scoreboard = SingleWidgetController(ScoreboardSettings(), WidgetManager, ::ScoreboardWidget)
    private val teamViews = TeamViewPosition.values().asList().associateWith { TeamViewController(WidgetManager, it) }
    fun teamView(position: TeamViewPosition): TeamViewController = teamViews[position]!!

    val locator = SingleWidgetController(TeamLocatorSettings(), WidgetManager, ::TeamLocatorWidget)

    private fun presetsPath(name: String) = config.presetsDirectory.resolve("$name.json")

    val advertisement = PresetsController(presetsPath("advertisements"), WidgetManager, ::AdvertisementWidget)
    val picture = PresetsController(presetsPath("pictures"), WidgetManager, ::PictureWidget)
    val title = PresetsController(presetsPath("title"), WidgetManager) { titleSettings: TitleSettings ->
        SvgWidget(
            Svg.loadAndSubstitute(config.mediaDirectory.resolve(titleSettings.preset), titleSettings.data)
        )
    }
    val tickerMessage = PresetsController(presetsPath("ticker"), TickerManager, TickerMessageSettings::toMessage)
}
