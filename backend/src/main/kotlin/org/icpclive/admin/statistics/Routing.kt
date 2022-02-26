package org.icpclive.admin.statistics

import io.ktor.routing.*
import org.icpclive.admin.setupSimpleWidgetRouting
import org.icpclive.admin.simpleWidgetViewFun
import org.icpclive.api.StatisticSettings
import org.icpclive.api.StatisticsWidget

fun Routing.configureStatistics() =
    setupSimpleWidgetRouting(
        prefix = "statistics",
        widgetId = StatisticsWidget.WIDGET_ID,
        presetPath = null,
        createWidget = {
            StatisticsWidget(StatisticSettings())
        },
        view = simpleWidgetViewFun<StatisticSettings>("Statistics") {},
    )