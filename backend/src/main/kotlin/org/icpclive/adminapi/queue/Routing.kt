package org.icpclive.adminapi.queue

import io.ktor.server.routing.*
import org.icpclive.adminapi.WidgetWrapper
import org.icpclive.adminapi.setupSimpleWidgetRouting
import org.icpclive.api.QueueSettings
import org.icpclive.api.QueueWidget


fun Routing.configureQueueApi() =
        setupSimpleWidgetRouting<QueueSettings, QueueWidget>(
                prefix = "queue",
                WidgetWrapper(createWidget = { QueueWidget(it) }, QueueSettings())
        )
