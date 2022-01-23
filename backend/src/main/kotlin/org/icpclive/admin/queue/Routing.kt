package org.icpclive.admin.queue

import io.ktor.routing.*
import org.icpclive.admin.*
import org.icpclive.api.QueueSettings
import org.icpclive.api.QueueWidget

fun Routing.configureQueue() =
    setupSimpleWidgetRouting(
        prefix = "queue",
        widgetId = QueueWidget.WIDGET_ID,
        presetPath = null,
        createWidget = {
            QueueWidget(QueueSettings())
        },
        view = simpleWidgetViewFun<QueueSettings>("Queue") {},
    )