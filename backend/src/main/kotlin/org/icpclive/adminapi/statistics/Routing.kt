package org.icpclive.adminapi.statistics

import io.ktor.routing.*
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.StatisticsSettings
import org.icpclive.api.StatisticsWidget


fun Routing.configureStatisticsApi() =
        setupSimpleWidgetRouting<StatisticsSettings, StatisticsWidget>(
                prefix = "statistics",
                createWidget = { StatisticsWidget(it) }
        )
