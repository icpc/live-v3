package org.icpclive.admin.picture

import kotlinx.html.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.admin.Presets
import org.icpclive.admin.SimpleWidgetUrls
import org.icpclive.admin.adminHead
import org.icpclive.api.Picture

fun HTML.pictureView(presets: Presets<Picture>, urls: SimpleWidgetUrls) {
    body {
        adminHead()
        h1 { + "Picture" }
        form {
            method = FormMethod.post
            action = urls.showQuery

            for (pic in presets.data.get()) {
                p {
                    radioInput {
                        name = "picture"
                        value = Json.encodeToString(pic)
                        +pic.name
                    }
                    img {
                        src = pic.url
                        height = "100px"
                    }
                }
            }
//            p {
//                radioInput {
//                    name = "text"
//                    value = "\$custom"
//                }
//                textArea {
//                    name = "customText"
//                }
//            }
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