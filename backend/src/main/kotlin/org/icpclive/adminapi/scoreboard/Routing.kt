package org.icpclive.adminapi.scoreboard

import io.ktor.routing.*
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.Scoreboard
import org.icpclive.api.ScoreboardSettings
import org.icpclive.api.ScoreboardWidget


fun Routing.configureScoreboardApi() =
        setupSimpleWidgetRouting<ScoreboardWidget>(
                prefix = "scoreboard",
                widgetId = ScoreboardWidget.WIDGET_ID,
                createWidget = { ScoreboardWidget(ScoreboardSettings()) }
        )
