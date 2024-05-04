package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.icpclive.admin.ApiActionException
import org.icpclive.api.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.util.completeOrThrow
import org.icpclive.util.getLogger
import org.icpclive.controllers.PresetsController
import org.icpclive.data.*
import org.icpclive.service.analytics.AnalyticsGenerator
import kotlin.time.Duration

sealed class AnalyticsAction {
    abstract val messageId: String
    data class CreateAdvertisement(override val messageId: String, val ttl: Duration?) : AnalyticsAction()
    data class DeleteAdvertisement(override val messageId: String, val expectedId: Int? = null) : AnalyticsAction()
    data class CreateTickerMessage(override val messageId: String, val ttl: Duration?) : AnalyticsAction()
    data class DeleteTickerMessage(override val messageId: String, val expectedId: Int? = null) : AnalyticsAction()
    data class MakeRunFeatured(override val messageId: String, val mediaType: TeamMediaType) : AnalyticsAction()
    data class MakeRunNotFeatured(override val messageId: String) : AnalyticsAction()
}


class AnalyticsService(val generator: AnalyticsGenerator) : Service {
    private val internalActions = MutableSharedFlow<AnalyticsAction>()
    private val messages = mutableMapOf<String, AnalyticsMessage>()

    private val resultFlow = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriberFlow = MutableStateFlow(0)

    private suspend fun modifyMessage(message: AnalyticsMessage) {
        messages[message.id] = message
        resultFlow.emit(ModifyAnalyticsMessageEvent(message))
    }

    private suspend fun <S : ObjectSettings, T : TypeWithId> AnalyticsCompanionPreset.hide(controller: PresetsController<S, T>) {
        controller.hideIfExists(this.presetId)
    }


    private suspend fun Action.process(featuredRunsFlow: FlowCollector<FeaturedRunAction>) {
        val message = messages[action.messageId]
        if (message == null) {
            logger.warn("Message with id ${action.messageId} not found")
            return
        }
        if (message !is AnalyticsCommentaryEvent) {
            logger.warn("Unsupported action for analytics message $message")
            return
        }
        when (action) {
            is AnalyticsAction.CreateAdvertisement -> {
                message.advertisement?.hide(Controllers.advertisement)
                val presetId = Controllers.advertisement.createWidget(
                    AdvertisementSettings(message.message),
                    action.ttl,
                    onDelete = { internalActions.emit(AnalyticsAction.DeleteAdvertisement(action.messageId, it)) }
                )
                Controllers.advertisement.show(presetId)
                modifyMessage(
                    message.copy(
                        advertisement = AnalyticsCompanionPreset(
                            presetId,
                            action.ttl?.let { Clock.System.now() + it }
                        )
                    )
                )
            }

            is AnalyticsAction.DeleteAdvertisement -> {
                if (action.expectedId == null || message.advertisement?.presetId == action.expectedId) {
                    message.advertisement?.hide(Controllers.advertisement)
                    modifyMessage(message.copy(advertisement = null))
                }
            }

            is AnalyticsAction.CreateTickerMessage -> {
                message.tickerMessage?.hide(Controllers.tickerMessage)
                val presetId = Controllers.tickerMessage.createWidget(
                    TextTickerSettings(TickerPart.LONG, 30000, message.message),
                    action.ttl,
                    onDelete = { internalActions.emit(AnalyticsAction.DeleteTickerMessage(action.messageId, it)) }
                )
                Controllers.tickerMessage.show(presetId)
                modifyMessage(
                    message.copy(
                        tickerMessage = AnalyticsCompanionPreset(
                            presetId,
                            action.ttl?.let { Clock.System.now() + it }
                        )
                    )
                )
            }

            is AnalyticsAction.DeleteTickerMessage -> {
                if (action.expectedId == null || message.tickerMessage?.presetId == action.expectedId) {
                    message.tickerMessage?.hide(Controllers.tickerMessage)
                    modifyMessage(message.copy(tickerMessage = null))
                }
            }

            is AnalyticsAction.MakeRunFeatured -> {
                if (message.runIds.size != 1 || message.teamIds.size != 1) {
                    logger.warn("Can't make run featured caused by message ${message.id}")
                    return
                }
                // TODO: it should be passed from above
                val team = DataBus.currentContestInfo().teams[message.teamIds[0]] ?: return
                val media = team.medias[action.mediaType] ?: return
                val request = FeaturedRunAction.MakeFeatured(message.runIds[0], media)
                featuredRunsFlow.emit(request)
                val companionRun = request.result.await() ?: return
                modifyMessage(message.copy(featuredRun = companionRun))
            }

            is AnalyticsAction.MakeRunNotFeatured -> {
                if (message.runIds.size != 1) {
                    logger.warn("Can't make run not featured caused by message ${message.id}")
                    return
                }
                featuredRunsFlow.emit(FeaturedRunAction.MakeNotFeatured(message.runIds[0]))
                modifyMessage(message.copy(featuredRun = null))
            }
        }
    }

    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            val featuredRunFlow = DataBus.queueFeaturedRunsFlow.await()
            val actionFlow = merge(DataBus.analyticsActionsFlow.await(), internalActions).map(::Action)
            logger.info("Analytics service is started")
            merge(generator.getFlow(flow).map(::Message), subscriberFlow.map { Subscribe }, actionFlow)
                .collect { event ->
                    when (event) {
                        is Message -> {
                            val message = event.message
                            messages[message.id] = message
                            resultFlow.emit(AddAnalyticsMessageEvent(message))
                        }

                        is Action -> {
                            try {
                                event.process(featuredRunFlow)
                            } catch (e: ApiActionException) {
                                logger.error("Failed during processing action: $event", e)
                            }
                        }

                        is Subscribe -> {
                            resultFlow.emit(AnalyticsMessageSnapshotEvent(messages.values.sortedBy { it.relativeTime }))
                        }
                    }
                }
        }
    }

    init {
        DataBus.analyticsFlow.completeOrThrow(flow {
            var needSnapshot = true
            resultFlow
                .onSubscription { subscriberFlow.update { it + 1 } }
                .collect {
                    if (it is AnalyticsMessageSnapshotEvent == needSnapshot) {
                        emit(it)
                        needSnapshot = false
                    }
                }
        })
    }

    companion object {
        val logger = getLogger(AnalyticsService::class)

        private sealed class AnalyticsProcessTrigger
        private data class Message(val message: AnalyticsMessage) : AnalyticsProcessTrigger()
        private data class Action(val action: AnalyticsAction) : AnalyticsProcessTrigger()
        private object Subscribe : AnalyticsProcessTrigger()
    }
}

