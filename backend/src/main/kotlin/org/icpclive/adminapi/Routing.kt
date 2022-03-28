package org.icpclive.adminapi

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.adminapi.advertisement.configureAdvertisementApi

private lateinit var topLevelLinks: List<Pair<String, String>>

internal fun BODY.adminApiHead() {
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

suspend inline fun ApplicationCall.catchAdminApiAction(block: ApplicationCall.() -> Unit) = try {
    block()
} catch (e: AdminActionApiException) {
    respond(mapOf("status" to "error", "message" to e.message))
}


fun Application.configureAdminApiRouting() {
    routing {
        val advertisementUrls =
            configureAdvertisementApi(environment.config.property("live.presets.advertisements").getString())

        topLevelLinks = listOf(
            advertisementUrls.mainPage to "Advertisement",
        )
        get("/adminapi") {
            call.respondRedirect(topLevelLinks[0].first)
        }
    }
}