package org.icpclive.admin.scoreboard

import io.ktor.server.routing.*
import org.icpclive.admin.setupSimpleWidgetRouting
import org.icpclive.admin.simpleWidgetViewFun
import org.icpclive.api.ScoreboardSettings
import org.icpclive.api.ScoreboardWidget

fun Routing.configureScoreboard() =
    setupSimpleWidgetRouting(
        prefix = "scoreboard",
        widgetId = ScoreboardWidget.WIDGET_ID,
        presetPath = null,
        createWidget = {
            ScoreboardWidget(ScoreboardSettings())
        },
        view = simpleWidgetViewFun<ScoreboardSettings>("Scoreboard") {},
    )