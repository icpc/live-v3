package org.icpclive.admin.advertisement

import io.ktor.routing.*
import kotlinx.html.p
import kotlinx.html.radioInput
import kotlinx.html.textArea
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
        view = simpleWidgetViewFun<Advertisement>("Advertisement") { presets ->
            for (ad in presets!!.data.get()) {
                p {
                    radioInput {
                        name = "text"
                        value = ad.text
                        +ad.text
                    }
                }
            }
            p {
                radioInput {
                    name = "text"
                    value = "\$custom"
                }
                textArea {
                    name = "customText"
                }
            }

        }
    )