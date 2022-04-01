package org.icpclive.adminapi.ticker

import io.ktor.routing.*
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.TickerSettings
import org.icpclive.api.TickerWidget


fun Routing.configureTickerApi() =
        setupSimpleWidgetRouting<TickerSettings, TickerWidget>(
                prefix = "ticker",
                createWidget = { TickerWidget(it) }
        )
