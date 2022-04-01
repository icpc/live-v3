package org.icpclive.adminapi.advertisement

import io.ktor.routing.*
import org.icpclive.adminapi.setupPresetWidgetRouting
import org.icpclive.api.AdvertisementSettings
import org.icpclive.api.AdvertisementWidget


fun Routing.configureAdvertisementApi(presetPath: String) =
        setupPresetWidgetRouting<AdvertisementSettings, AdvertisementWidget>(
                prefix = "advertisement",
                presetPath = presetPath,
                createWidget = { AdvertisementWidget(it) }
        )
