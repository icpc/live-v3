package org.icpclive.adminapi.ticker

import io.ktor.routing.*
import org.icpclive.adminapi.WidgetWrapper
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.adminapi.setupPresetTickerRouting
import org.icpclive.api.*


fun Routing.configureTickerApi() =
        setupSimpleWidgetRouting<TickerSettings, TickerWidget>(
                prefix = "ticker",
                WidgetWrapper(createWidget = { TickerWidget(it) }, TickerSettings())
        )

fun Routing.configureTickerMessagesApi(presetPath: String) =
        setupPresetTickerRouting(
                prefix = "tickermessage",
                presetPath = presetPath,
                createMessage = {
                    when (it) {
                        is TextTickerSettings -> TextTickerMessage(it)
                        is ClockTickerSettings -> ClockTickerMessage(it)
                        is ScoreboardTickerSettings -> ScoreboardTickerMessage(it)
                        else -> TODO("Some bug in sealed class")
                    }
                }
        )
