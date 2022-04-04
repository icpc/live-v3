package org.icpclive.adminapi.picture

import io.ktor.server.routing.*
import org.icpclive.adminapi.setupPresetWidgetRouting
import org.icpclive.api.PictureSettings
import org.icpclive.api.PictureWidget


fun Routing.configurePictureApi(presetPath: String) =
        setupPresetWidgetRouting<PictureSettings, PictureWidget>(
                prefix = "picture",
                presetPath = presetPath,
                createWidget = { PictureWidget(it) }
        )
