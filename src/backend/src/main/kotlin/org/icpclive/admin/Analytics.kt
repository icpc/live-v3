package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.api.CommentaryMessageId
import org.icpclive.cds.api.toCommentaryMessageId
import org.icpclive.cds.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfoFlow
import org.icpclive.service.AnalyticsAction
import org.icpclive.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun Route.setupAnalytics() {
    val actionsFlow = MutableSharedFlow<AnalyticsAction>(extraBufferCapacity = 10000)
    DataBus.analyticsActionsFlow.completeOrThrow(actionsFlow)

    fun ApplicationCall.id() = parameters["id"]?.toAnalyticsMessageId() ?: throw ApiActionException("Error load analytics message by id")
    fun ApplicationCall.commentId() = parameters["commentId"]?.toCommentaryMessageId() ?: throw ApiActionException("Error load analytics message comment by id")

    fun Route.presetWidget(
        name: String,
        showAction: (id: AnalyticsMessageId, commentId: CommentaryMessageId, ttlMs: Duration?) -> AnalyticsAction,
        hideAction: (id: AnalyticsMessageId, commentId: CommentaryMessageId) -> AnalyticsAction,
    ) {
        route("/{commentId}/$name") {
            post {
                call.adminApiAction {
                    actionsFlow.emit(showAction(call.id(), call.commentId(), call.request.queryParameters["ttl"]?.toLong()?.milliseconds))
                }
            }
            delete {
                call.adminApiAction {
                    actionsFlow.emit(hideAction(call.id(), call.commentId()))
                }
            }
        }
    }

    webSocket { sendJsonFlow(DataBus.analyticsFlow.await()) }
    get { call.respond(DataBus.analyticsFlow.await().filterIsInstance<AnalyticsMessageSnapshotEvent>().first().messages) }

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
        post("/featuredRun") {
            call.adminApiAction {
                actionsFlow.emit(AnalyticsAction.MakeRunFeatured(call.id(), call.safeReceive()))
            }
        }
        delete("/featuredRun") {
            call.adminApiAction {
                actionsFlow.emit(AnalyticsAction.MakeRunNotFeatured(call.id()))
            }
        }

    }

    get("/contestInfo") { call.respond(DataBus.currentContestInfoFlow().first()) }
}
