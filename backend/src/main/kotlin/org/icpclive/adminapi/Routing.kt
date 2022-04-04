package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.ticker
import kotlinx.html.*
import org.icpclive.adminapi.advertisement.*
import org.icpclive.adminapi.picture.*
import org.icpclive.adminapi.scoreboard.*
import org.icpclive.adminapi.queue.*
import org.icpclive.adminapi.statistics.*
import org.icpclive.adminapi.ticker.*

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
                configureAdvertisementApi(environment!!.config.property("live.presets.advertisements").getString())
        val pictureUrls =
                configurePictureApi(environment!!.config.property("live.presets.pictures").getString())

        val scoreboardUrls = configureScoreboardApi()
        val queueUrls = configureQueueApi()
        val tickerUrls = configureTickerApi()
        val statisticsUrls = configureStatisticsApi()

        topLevelLinks = listOf(
                advertisementUrls.mainPage to "Advertisement",
                pictureUrls.mainPage to "Picture",
                scoreboardUrls.mainPage to "Scoreboard",
                statisticsUrls.mainPage to "Statistics",
                queueUrls.mainPage to "Queue",
                tickerUrls.mainPage to "Ticker",
        )
    }
}