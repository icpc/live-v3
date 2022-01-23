package org.icpclive.admin

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.admin.advertisement.configureAdvertisement
import org.icpclive.admin.overlayEvents.configureOverlayEvents
import org.icpclive.admin.picture.configurePicture
import org.icpclive.admin.queue.configureQueue

private lateinit var topLevelLinks: List<Pair<String, String>>

internal fun BODY.adminHead() {
    table {
        tr {
            for ((url, text) in topLevelLinks) {
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
        val queueUrls = configureQueue()

        topLevelLinks = listOf(
            advertisementUrls.mainPage to "Advertisement",
            pictureUrls.mainPage to "Picture",
            overlayEventsUrls.mainPage to "Events",
            queueUrls.mainPage to "Queue"
        )
        get("/admin") { call.respondRedirect(topLevelLinks[0].first) }
    }
}