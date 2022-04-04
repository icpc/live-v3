package org.icpclive.adminapi.ticker

import io.ktor.server.routing.*
import org.icpclive.adminapi.WidgetWrapper
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.TickerSettings
import org.icpclive.api.TickerWidget


fun Routing.configureTickerApi() =
        setupSimpleWidgetRouting<TickerSettings, TickerWidget>(
                prefix = "ticker",
                WidgetWrapper(createWidget = { TickerWidget(it) }, TickerSettings())
        )
