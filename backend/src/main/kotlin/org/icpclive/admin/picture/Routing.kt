package org.icpclive.admin.picture

import io.ktor.routing.*
import kotlinx.html.HTML
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.admin.*
import org.icpclive.api.Picture
import org.icpclive.api.PictureWidget

fun Routing.configurePicture(presetPath: String) =
    setupSimpleWidgetRouting(
        prefix = "picture",
        widgetId = PictureWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = { parameters ->
            val picture = parameters["picture"]?.let { Json.decodeFromString<Picture>(it) } ?: throw AdminActionException("No picture chosen")
            PictureWidget(picture)
        },
        view = HTML::pictureView
    )