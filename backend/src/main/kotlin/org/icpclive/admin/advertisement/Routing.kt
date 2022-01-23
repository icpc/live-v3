package org.icpclive.admin.advertisement

import io.ktor.routing.*
import kotlinx.html.HTML
import org.icpclive.admin.*
import org.icpclive.api.Advertisement
import org.icpclive.api.AdvertisementWidget


fun Routing.configureAdvertisement(presetPath: String) =
    setupSimpleWidgetRouting(
        prefix = "advertisement",
        widgetId = AdvertisementWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = { parameters ->
            val text = parameters["text"].let {
                if (it == "\$custom") {
                    parameters["customText"]
                } else {
                    it
                }
            } ?: throw AdminActionException("No advertisement chosen")
            AdvertisementWidget(Advertisement(text))
        },
        view = HTML::advertisementsView
    )