package org.icpclive.adminapi.advertisement

import io.ktor.routing.*
import org.icpclive.adminapi.setupPresetWidgetRouting
import org.icpclive.api.AdvertisementSettings
import org.icpclive.api.AdvertisementWidget


fun Routing.configureAdvertisementApi(presetPath: String) =
    setupPresetWidgetRouting<AdvertisementSettings, AdvertisementWidget>(
        prefix = "advertisement",
        widgetId = AdvertisementWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = { AdvertisementWidget(it) }
    )
