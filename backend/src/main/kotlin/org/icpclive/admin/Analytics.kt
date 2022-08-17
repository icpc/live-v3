package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.data.DataBus
import org.icpclive.service.AnalyticsAction
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.sendJsonFlow

fun Route.setupAnalytics() {
    val actionsFlow = MutableSharedFlow<AnalyticsAction>(extraBufferCapacity = 10000)
    DataBus.analyticsActionsFlow.completeOrThrow(actionsFlow)

    fun ApplicationCall.id() =
        parameters["id"] ?: throw ApiActionException("Error load preset by id")

    fun Route.presetWidget(
        name: String,
        showAction: (id: String, ttlMs: Long?) -> AnalyticsAction,
        hideAction: (id: String) -> AnalyticsAction
    ) {
        route("/$name") {
            post {
                call.adminApiAction {
                    actionsFlow.emit(showAction(call.id(), call.request.queryParameters["ttl"]?.toLong()))
                }
            }
            delete {
                call.adminApiAction {
                    actionsFlow.emit(hideAction(call.id()))
                }
            }
        }
    }

    webSocket { sendJsonFlow(DataBus.analyticsFlow.await()) }
    route("/{id}") {
        presetWidget(
            "advertisement",
            AnalyticsAction::CreateAdvertisement,
            AnalyticsAction::DeleteAdvertisement
        )
        presetWidget(
            "tickerMessage",
            AnalyticsAction::CreateTickerMessage,
            AnalyticsAction::DeleteTickerMessage
        )
    }
}
