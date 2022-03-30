package org.icpclive.adminapi.advertisement

import io.ktor.routing.*
import org.icpclive.adminapi.setupPresetWidgetRouting
import org.icpclive.api.Advertisement
import org.icpclive.api.AdvertisementWidget


fun Routing.configureAdvertisementApi(presetPath: String) =
    setupPresetWidgetRouting<Advertisement, AdvertisementWidget>(
        prefix = "advertisement",
        widgetId = AdvertisementWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = { AdvertisementWidget(it) }
    )
