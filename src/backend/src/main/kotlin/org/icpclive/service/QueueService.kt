package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.data.DataBus
import org.icpclive.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private sealed class QueueProcessTrigger
private data object Clean : QueueProcessTrigger()
private data class Event(val state: ContestState) : QueueProcessTrigger()
private data class Featured(val request: FeaturedRunAction) : QueueProcessTrigger()
private data object Subscribe : QueueProcessTrigger()

sealed class FeaturedRunAction(val runId: RunId) {
    class MakeFeatured(
        runId: RunId,
        val mediaType: MediaType,
    ) : FeaturedRunAction(runId) {
        val result: CompletableDeferred<AnalyticsCompanionRun?> = CompletableDeferred()
    }

    class MakeNotFeatured(runId: RunId) : FeaturedRunAction(runId)
}

class QueueService : Service {
    private val runs = mutableMapOf<RunId, RunInfo>()
    private val removedRuns = mutableMapOf<RunId, RunInfo>()
    private var featuredRun: FeaturedRunInfo? = null
    private val lastUpdateTime = mutableMapOf<RunId, Duration>()

    private val resultFlow = MutableSharedFlow<QueueEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val subscriberFlow = MutableStateFlow(0)

    init {
        DataBus.queueFlow.completeOrThrow(flow {
            var nothingSent = true
            resultFlow
                .onSubscription { subscriberFlow.update { it + 1 } }
                .collect {
                    val isSnapshot = it is QueueSnapshotEvent
                    if (nothingSent == isSnapshot) {
                        emit(it)
                        nothingSent = false
                    }
                }
        })
    }

    private suspend fun modifyRun(rawRun: RunInfo, sendToOverlay: Boolean = true) {
        val featuredMediaType = featuredRun?.takeIf { it.runId == rawRun.id }?.mediaType
        val run = rawRun.takeIf { it.featuredRunMedia == featuredMediaType }
            ?: rawRun.copy(featuredRunMedia = featuredMediaType)
        if (sendToOverlay) {
            resultFlow.emit(if (run.id in runs) ModifyRunInQueueEvent(run) else AddRunToQueueEvent(run))
        }
        runs[run.id] = run
    }

    private suspend fun FeaturedRunInfo.makeNotFeatured() {
        val run = runs[runId] ?: removedRuns[runId] ?: return
        featuredRun = null
        modifyRun(run.copy(featuredRunMedia = null), runId in runs)
    }

    private suspend fun removeRun(run: RunInfo) {
        runs.remove(run.id)
        featuredRun?.takeIf { it.runId == run.id }?.makeNotFeatured()
        removedRuns[run.id] = run
        resultFlow.emit(RemoveRunFromQueueEvent(run))
    }

    private val RunInfo.timeInQueue
        get() = when {
            featuredRunMedia != null -> FEATURED_WAIT_TIME
            (result as? RunResult.ICPC)?.isFirstToSolveRun == true -> FIRST_TO_SOLVE_WAIT_TIME
            else -> WAIT_TIME
        }

    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            val featuredRunsFlow = MutableSharedFlow<FeaturedRunAction>(
                extraBufferCapacity = 100,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            DataBus.queueFeaturedRunsFlow.completeOrThrow(featuredRunsFlow)
            logger.info("Queue service is started")
            var firstEventTime: Duration? = null
            val removerFlowTrigger = loopFlow(1.seconds, onError = {}) { Clean }
            val statesFlowTrigger = flow.map { Event(it.state) }
            val subscriberFlowTrigger = subscriberFlow.map { Subscribe }
            val featuredFlowTrigger = featuredRunsFlow.map { Featured(it) }
            var currentContestInfo: ContestInfo? = null
            // it's important to have all side effects after merge, as part before merge will be executed concurrently
            merge(statesFlowTrigger, removerFlowTrigger, subscriberFlowTrigger, featuredFlowTrigger).collect { event ->
                when (event) {
                    is Clean -> {
                        val currentTime = currentContestInfo?.currentContestTime ?: return@collect
                        runs.values
                            .filter { currentTime >= lastUpdateTime[it.id]!! + it.timeInQueue }
                            .filterNot { it.featuredRunMedia != null }
                            .forEach { removeRun(it) }
                    }

                    is Event -> {
                        when (val update = event.state.lastEvent) {
                            is AnalyticsUpdate -> {}
                            is InfoUpdate -> {}
                            is RunUpdate -> {
                                val contestInfo = event.state.infoAfterEvent?.takeIf { it.status != ContestStatus.BEFORE } ?: return@collect
                                currentContestInfo = contestInfo
                                val run = update.newInfo
                                removedRuns[run.id] = run
                                if (firstEventTime == null) {
                                    firstEventTime = contestInfo.currentContestTime
                                }
                                val currentTime = contestInfo.currentContestTime.takeIf { it > firstEventTime!! + 60.seconds } ?: run.time
                                lastUpdateTime[run.id] = currentTime
                                if (run.isHidden) {
                                    if (run.id in runs) {
                                        removeRun(run)
                                    }
                                } else {
                                    if (run.id in runs || contestInfo.currentContestTime <= currentTime + run.timeInQueue) {
                                        modifyRun(run)
                                    } else {
                                        logger.debug("Ignore run as it is too old: ${contestInfo.currentContestTime} vs ${currentTime + run.timeInQueue}")
                                    }
                                }
                            }
                        }
                    }

                    is Featured -> {
                        val runId = event.request.runId
                        val run = runs[runId] ?: removedRuns[runId]
                        if (run == null) {
                            logger.warn("There is no run with id $runId for make it featured")
                            if (event.request is FeaturedRunAction.MakeFeatured) {
                                event.request.result.complete(null)
                            }
                            return@collect
                        }
                        when (event.request) {
                            is FeaturedRunAction.MakeFeatured -> {
                                featuredRun?.makeNotFeatured()
                                featuredRun = FeaturedRunInfo(run.id, event.request.mediaType)
                                modifyRun(run)
                                lastUpdateTime[run.id] = run.time
                                event.request.result.complete(
                                    AnalyticsCompanionRun(Clock.System.now() + FEATURED_WAIT_TIME, event.request.mediaType)
                                )
                            }

                            is FeaturedRunAction.MakeNotFeatured -> {
                                if (featuredRun?.runId == run.id) {
                                    featuredRun = null
                                    modifyRun(run, runId in runs)
                                }
                            }
                        }
                    }

                    is Subscribe -> {
                        resultFlow.emit(QueueSnapshotEvent(runs.values.sortedBy { it.time }))
                    }
                }
                while (runs.size >= MAX_QUEUE_SIZE) {
                    runs.values.asSequence()
                        .filterNot { (it.result as? RunResult.ICPC)?.isFirstToSolveRun == true || it.featuredRunMedia != null }
                        .minByOrNull { it.time }
                        ?.run { removeRun(this) }
                        ?: break
                }
            }
        }
    }

    companion object {
        val logger = getLogger(QueueService::class)

        private data class FeaturedRunInfo(val runId: RunId, val mediaType: MediaType)

        private val WAIT_TIME = 1.minutes
        private val FIRST_TO_SOLVE_WAIT_TIME = 2.minutes
        private val FEATURED_WAIT_TIME = 1.minutes
        private const val MAX_QUEUE_SIZE = 10
    }
}
