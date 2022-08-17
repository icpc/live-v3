package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.data.DataBus
import org.icpclive.service.AnalyticsService
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.sendJsonFlow

fun Route.setupAnalytics() {
    val actionsFlow = MutableSharedFlow<AnalyticsService.Companion.AnalyticsAction>(extraBufferCapacity = 10000)
    DataBus.analyticsActionsFlow.completeOrThrow(actionsFlow)

    fun ApplicationCall.id() =
        parameters["id"] ?: throw ApiActionException("Error load preset by id")

    fun Route.presetWidget(
        name: String,
        showAction: (id: String, ttlMs: Long?) -> AnalyticsService.Companion.AnalyticsAction,
        hideAction: (id: String) -> AnalyticsService.Companion.AnalyticsAction
    ) {
        route("/$name") {
            post("/{id}") {
                call.adminApiAction {
                    actionsFlow.emit(showAction(call.id(), call.request.queryParameters["ttl"]?.toLong()))
                }
            }
            delete("/{id}") {
                call.adminApiAction {
                    actionsFlow.emit(hideAction(call.id()))
                }
            }
        }
    }

    webSocket { sendJsonFlow(DataBus.analyticsFlow.await()) }
    presetWidget(
        "advertisement",
        AnalyticsService.Companion::ShowAnalyticsAdvertisement,
        AnalyticsService.Companion::HideAnalyticsAdvertisement
    )
    presetWidget(
        "tickerMessage",
        AnalyticsService.Companion::ShowAnalyticsTickerMessage,
        AnalyticsService.Companion::HideAnalyticsTickerMessage
    )
}
