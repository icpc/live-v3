package org.icpclive.admin.advertisement

import kotlinx.html.*
import org.icpclive.admin.Presets
import org.icpclive.admin.SimpleWidgetUrls
import org.icpclive.admin.adminHead
import org.icpclive.api.Advertisement

fun HTML.advertisementsView(presets: Presets<Advertisement>, urls: SimpleWidgetUrls) {
    body {
        adminHead()
        h1 { +"Advertisement" }
        form {
            method = FormMethod.post
            action = urls.showQuery

            for (ad in presets.data.get()) {
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
            submitInput { value = "Show" }
        }
        form {
            method = FormMethod.post
            action = urls.hideQuery
            submitInput { value = "Hide" }
        }
        form {
            method = FormMethod.post
            action = urls.reloadQuery
            submitInput { value = "Reload" }
        }
    }
}