package org.icpclive.admin

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.admin.advertisement.configureAdvertisement
import org.icpclive.admin.overlayEvents.configureOverlayEvents
import org.icpclive.admin.picture.configurePicture

private val topLevelLinks = mutableMapOf<String, String>()

internal fun BODY.adminHead() {
    table {
        tr {
            for ((url, text) in topLevelLinks.entries.sortedBy { it.key }) {
                td {
                    a(url) { +text }
                }
            }
        }
    }
}

suspend inline fun ApplicationCall.catchAdminAction(back: String, block: ApplicationCall.() -> Unit) = try {
    block()
} catch (e: AdminActionException) {
    respondHtml {
        body {
            h1 {
                +"Error: ${e.message}"
            }
            a {
                href = back
                +"Back"
            }
        }
    }
}


fun Application.configureAdminRouting() {
    routing {
        val advertisementUrls =
            configureAdvertisement(environment.config.property("live.presets.advertisements").getString())
        val pictureUrls = configurePicture(environment.config.property("live.presets.pictures").getString())
        val overlayEventsUrls = configureOverlayEvents()

        topLevelLinks[advertisementUrls.mainPage] = "Advertisement"
        topLevelLinks[pictureUrls.mainPage] = "Picture"
        topLevelLinks[overlayEventsUrls.mainPage] = "Events"
    }
}