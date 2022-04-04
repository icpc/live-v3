package org.icpclive.adminapi.statistics

import io.ktor.server.routing.*
import org.icpclive.adminapi.WidgetWrapper
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.StatisticsSettings
import org.icpclive.api.StatisticsWidget


fun Routing.configureStatisticsApi() =
        setupSimpleWidgetRouting<StatisticsSettings, StatisticsWidget>(
                prefix = "statistics",
                WidgetWrapper(createWidget = { StatisticsWidget(it) }, StatisticsSettings())
        )
