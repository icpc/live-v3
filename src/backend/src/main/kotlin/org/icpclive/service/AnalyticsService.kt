package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.icpclive.admin.ApiActionException
import org.icpclive.api.*
import org.icpclive.api.AnalyticsMessage
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.util.completeOrThrow
import org.icpclive.cds.util.getLogger
import org.icpclive.controllers.PresetsController
import org.icpclive.data.*
import org.icpclive.service.analytics.AnalyticsGenerator
import kotlin.time.Duration

sealed class AnalyticsAction {
    abstract val messageId: AnalyticsMessageId

    sealed class AnalyticsCommentaryAction : AnalyticsAction() {
        abstract val commentId: String
    }

    data class CreateAdvertisement(override val messageId: AnalyticsMessageId, override val commentId: String, val ttl: Duration?) : AnalyticsCommentaryAction()
    data class DeleteAdvertisement(override val messageId: AnalyticsMessageId, override val commentId: String, val expectedId: Int? = null) : AnalyticsCommentaryAction()
    data class CreateTickerMessage(override val messageId: AnalyticsMessageId, override val commentId: String, val ttl: Duration?) : AnalyticsCommentaryAction()
    data class DeleteTickerMessage(override val messageId: AnalyticsMessageId, override val commentId: String, val expectedId: Int? = null) : AnalyticsCommentaryAction()

    data class MakeRunFeatured(override val messageId: AnalyticsMessageId, val mediaType: TeamMediaType) : AnalyticsAction()
    data class MakeRunNotFeatured(override val messageId: AnalyticsMessageId) : AnalyticsAction()
}


class AnalyticsService(private val generator: AnalyticsGenerator) : Service {
    private val internalActions = MutableSharedFlow<AnalyticsAction>()
    private var contestInfo: ContestInfo? = null
    private val messages = mutableMapOf<AnalyticsMessageId, AnalyticsMessage>()

    private val resultFlow = MutableSharedFlow<AnalyticsEvent>(
        extraBufferCapacity = 50000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val subscriberFlow = MutableStateFlow(0)

    private fun AnalyticsAction.MakeRunFeatured.getMediaForFeatured(run: RunInfo): MediaType? {
        val team = contestInfo?.teams?.get(run.teamId) ?: return null
        if (mediaType == TeamMediaType.REACTION_VIDEO) {
            if (run.reactionVideos.isNotEmpty()) {
                return run.reactionVideos[0]
            }
        } else {
            val media = team.medias[mediaType]
            if (media != null) return media
        }
        log.warning { "Can't make run ${run.id} with missing media $mediaType" }
        return null
    }

    private suspend fun <S : ObjectSettings, T : TypeWithId> AnalyticsCompanionPreset.hide(controller: PresetsController<S, T>) {
        controller.hideIfExists(this.presetId)
    }

    private suspend fun Action.process(featuredRunsFlow: FlowCollector<FeaturedRunAction>) {
        val message = messages[action.messageId]
        if (message == null) {
            log.warning { "AnalyticsMessage with id ${action.messageId} not found" }
            return
        }
        when (action) {
            is AnalyticsAction.AnalyticsCommentaryAction -> {
                val comment = message.comments.find { it.id == action.commentId }
                if (comment == null) {
                    log.warning { "AnalyticsMessageCommentary with id ${action.messageId} (${message.id}) not found" }
                    return
                }

                updateMessage(
                    message,
                    comment = when (action) {
                        is AnalyticsAction.CreateAdvertisement -> {
                            comment.advertisement?.hide(Controllers.advertisement)
                            val presetId = Controllers.advertisement.createWidget(
                                AdvertisementSettings(comment.message),
                                action.ttl,
                                onDelete = { internalActions.emit(AnalyticsAction.DeleteAdvertisement(action.messageId, action.commentId, it)) }
                            )
                            Controllers.advertisement.show(presetId)
                            comment.copy(advertisement = AnalyticsCompanionPreset(presetId, action.ttl?.let { Clock.System.now() + it }))
                        }

                        is AnalyticsAction.DeleteAdvertisement -> {
                            if (action.expectedId == null || comment.advertisement?.presetId == action.expectedId) {
                                comment.advertisement?.hide(Controllers.advertisement)
                                comment.copy(advertisement = null)
                            } else {
                                comment
                            }
                        }

                        is AnalyticsAction.CreateTickerMessage -> {
                            comment.tickerMessage?.hide(Controllers.tickerMessage)
                            val presetId = Controllers.tickerMessage.createWidget(
                                TextTickerSettings(TickerPart.LONG, 30000, comment.message),
                                action.ttl,
                                onDelete = { internalActions.emit(AnalyticsAction.DeleteTickerMessage(action.messageId, action.commentId, it)) }
                            )
                            Controllers.tickerMessage.show(presetId)
                            comment.copy(tickerMessage = AnalyticsCompanionPreset(presetId, action.ttl?.let { Clock.System.now() + it }))
                        }

                        is AnalyticsAction.DeleteTickerMessage -> {
                            if (action.expectedId == null || comment.tickerMessage?.presetId == action.expectedId) {
                                comment.tickerMessage?.hide(Controllers.tickerMessage)
                                comment.copy(tickerMessage = null)
                            } else {
                                comment
                            }
                        }
                    }
                )
            }

            is AnalyticsAction.MakeRunFeatured -> {
                val run = message.runInfo
                if (run == null) {
                    log.warning { "Can't make run featured caused by message ${message.id}" }
                    return
                }
                val media = action.getMediaForFeatured(run) ?: return
                val request = FeaturedRunAction.MakeFeatured(run.id, media)
                featuredRunsFlow.emit(request)
                val companionRun = request.result.await() ?: return
                updateMessage(message, featuredRun = companionRun)
            }

            is AnalyticsAction.MakeRunNotFeatured -> {
                val run = message.runInfo
                if (run == null) {
                    log.warning { "Can't make run not featured caused by message ${message.id}" }
                    return
                }
                featuredRunsFlow.emit(FeaturedRunAction.MakeNotFeatured(run.id))
                updateMessage(message, featuredRunRemove = true)
            }
        }
    }

    private suspend fun updateMessage(
        message: AnalyticsMessage,
        run: RunInfo? = null,
        comment: AnalyticsMessageComment? = null,
        commentEvent: CommentaryMessage? = null,
        featuredRun: AnalyticsCompanionRun? = null,
        featuredRunRemove: Boolean? = false,
    ) {
        var updateTime = message.lastUpdateTime
        var time = message.time
        var relativeTime = message.relativeTime
        var teamId = message.teamId
        var runInfo = message.runInfo
        var comments = message.comments
        if (commentEvent != null) {
            updateTime = maxOf(updateTime, commentEvent.time)
            comments = comments.updateComment(AnalyticsMessageComment(commentEvent.id, commentEvent.message, creationTime = commentEvent.time))
        }
        if (run != null) {
            teamId = run.teamId
            runInfo = run
        }
        runInfo?.let { // actual time of run
            time = contestInfo?.instantAt(it.time) ?: time
            relativeTime = it.time
            updateTime = maxOf(updateTime, time)
        }

        if (comment != null) {
            comments = comments.updateComment(comment)
        }
        val newMessage = message.copy(
            lastUpdateTime = updateTime,
            time = time,
            relativeTime = relativeTime,
            teamId = teamId,
            runInfo = runInfo,
            comments = comments,
            featuredRun = featuredRun ?: message.featuredRun.takeIf { featuredRunRemove != true }
        )
        messages[message.id] = newMessage
        resultFlow.emit(UpdateAnalyticsMessageEvent(newMessage))
    }

    private fun List<AnalyticsMessageComment>.updateComment(comment: AnalyticsMessageComment) =
        (filterNot { it.id == comment.id } + comment).sortedByDescending { it.creationTime }

    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            val featuredRunFlow = DataBus.queueFeaturedRunsFlow.await()
            val actionFlow = merge(DataBus.analyticsActionsFlow.await(), internalActions).map(::Action)
            log.info { "Analytics service is started" }
            merge(
                subscriberFlow.map { Subscribe },
                actionFlow,
                generator.getFlow(flow).map { ContestUpdate(AnalyticsUpdate(it)) },
                flow.map { ContestUpdate(it.state.lastEvent) }
            ).collect { event ->
                when (event) {
                    is ContestUpdate -> {
                        when (val update = event.update) {
                            is InfoUpdate -> {
                                contestInfo = update.newInfo
                            }

                            is RunUpdate -> {

                                val run = update.newInfo
                                if (run.isHidden) return@collect

                                val messageId = AnalyticsMessageIdRun(run.id)
                                val timeInstant = contestInfo?.instantAt(run.time) ?: Clock.System.now()
                                val message = messages[messageId] ?: AnalyticsMessage(
                                    id = messageId,
                                    lastUpdateTime = timeInstant,
                                    time = timeInstant,
                                    relativeTime = run.time
                                )
                                updateMessage(message, run = run)

                            }

                            is AnalyticsUpdate -> {
                                val analyticsMessage = update.message
                                for (runId in analyticsMessage.runIds) {
                                    val messageId = AnalyticsMessageIdRun(runId)
                                    val message = messages[messageId] ?: AnalyticsMessage(
                                        id = messageId,
                                        lastUpdateTime = analyticsMessage.time,
                                        time = analyticsMessage.time,
                                        relativeTime = analyticsMessage.relativeTime
                                    )
                                    updateMessage(message, commentEvent = analyticsMessage)
                                }
                                // also we can iterate of teamId in Commentary message and create Analytics message for each team
                                if (analyticsMessage.runIds.isEmpty()) {
                                    val messageId = AnalyticsMessageIdCommentary(analyticsMessage.id)
                                    val message = messages[messageId] ?: AnalyticsMessage(
                                        id = messageId,
                                        lastUpdateTime = analyticsMessage.time,
                                        time = analyticsMessage.time,
                                        relativeTime = analyticsMessage.relativeTime
                                    )
                                    updateMessage(message, commentEvent = analyticsMessage /* todo: team??? */)
                                }
                            }
                        }
                    }

                    is Action -> {
                        try {
                            event.process(featuredRunFlow)
                        } catch (e: ApiActionException) {
                            log.error(e) { "Failed during processing action: $event" }
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
        val log by getLogger()

        private sealed class AnalyticsProcessTrigger
        private data class ContestUpdate(val update: org.icpclive.cds.ContestUpdate) : AnalyticsProcessTrigger()
        private data class Action(val action: AnalyticsAction) : AnalyticsProcessTrigger()
        private data object Subscribe : AnalyticsProcessTrigger()
    }
}

