package org.icpclive.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.WidgetControllers
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.getLogger

class AnalyticsService {
    private val messages = mutableMapOf<String, AnalyticsMessage>()

    private val resultFlow = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriberFlow = MutableStateFlow(0)

    private suspend fun Action.process(): AnalyticsMessage? {
        val message = messages[action.messageId]
        if (message == null) {
            logger.warn("Message with id ${action.messageId} not found")
            return null
        }
        if (message !is AnalyticsCommentaryEvent) {
            logger.warn("Unsupported action for analytics message $message")
            return null
        }
        when (action) {
            is ShowAnalyticsAdvertisement -> {
                if (message.advertisementId != null) {
                    message.advertisementId?.let { WidgetControllers.advertisement.delete(it) }
                }
                message.advertisementId = WidgetControllers.advertisement.createAndShowWithTtl(
                    AdvertisementSettings(message.message),
                    action.ttlMs
                )
            }
            is HideAnalyticsAdvertisement -> {
                message.advertisementId?.let { WidgetControllers.advertisement.delete(it) }
            }
            is ShowAnalyticsTickerMessage -> {
                if (message.tickerMessageId != null) {
                    message.tickerMessageId?.let { WidgetControllers.tickerMessage.delete(it) }
                }
                message.tickerMessageId = WidgetControllers.tickerMessage.createAndShowWithTtl(
                    TextTickerSettings(TickerPart.LONG, 30000, message.message),
                    action.ttlMs
                )
            }
            is HideAnalyticsTickerMessage -> {
                message.tickerMessageId?.let { WidgetControllers.tickerMessage.delete(it) }
            }
        }
        return message
    }

    suspend fun run(rawEvents: Flow<AnalyticsMessage>) {
        val actionFlow = DataBus.analyticsActionsFlow.await().map(::Action)
        logger.info("Analytics service is started")
        merge(rawEvents.map(::Message), subscriberFlow.map { Subscribe }, actionFlow).collect { event ->
            when (event) {
                is Message -> {
                    val message = event.message
                    messages[message.id] = message
                    resultFlow.emit(AddAnalyticsMessageEvent(message))
                }
                is Action -> {
                    event.process()?.let { resultFlow.emit(ModifyAnalyticsMessageEvent(it)) }
                }
                is Subscribe -> {
                    resultFlow.emit(AnalyticsMessageSnapshotEvent(messages.values.sortedBy { it.relativeTime }))
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
        val logger = getLogger(AnalyticsMessage::class)

        private sealed class AnalyticsProcessTrigger
        private class Message(val message: AnalyticsMessage) : AnalyticsProcessTrigger()
        private class Action(val action: AnalyticsAction) : AnalyticsProcessTrigger()
        private object Subscribe : AnalyticsProcessTrigger()

        sealed class AnalyticsAction(val messageId: String)
        class ShowAnalyticsAdvertisement(messageId: String, val ttlMs: Long?) : AnalyticsAction(messageId)
        class HideAnalyticsAdvertisement(messageId: String) : AnalyticsAction(messageId)
        class ShowAnalyticsTickerMessage(messageId: String, val ttlMs: Long?) : AnalyticsAction(messageId)
        class HideAnalyticsTickerMessage(messageId: String) : AnalyticsAction(messageId)
    }
}
