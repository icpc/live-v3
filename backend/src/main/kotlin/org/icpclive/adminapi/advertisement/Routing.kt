package org.icpclive.adminapi.advertisement

import io.ktor.routing.*
import org.icpclive.admin.AdminActionException
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.Advertisement
import org.icpclive.api.AdvertisementWidget


fun Routing.configureAdvertisementApi(presetPath: String) =
    setupSimpleWidgetRouting<Advertisement, AdvertisementWidget>(
        prefix = "advertisement",
        widgetId = AdvertisementWidget.WIDGET_ID,
        presetPath = presetPath,
        createContent = { parameters ->
            val text = parameters["text"] ?: throw AdminActionException("No advertisement chosen")
            Advertisement(text)
        },
        createWidget = { AdvertisementWidget(it) }
    )
