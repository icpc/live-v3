package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import org.icpclive.api.*
import org.icpclive.common.util.completeOrThrow
import org.icpclive.common.util.getLogger
import org.icpclive.common.util.intervalFlow
import org.icpclive.data.DataBus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private sealed class QueueProcessTrigger
private object Clean : QueueProcessTrigger()
private class Run(val run: RunInfo) : QueueProcessTrigger()
private class Featured(val request: FeaturedRunAction) : QueueProcessTrigger()
private object Subscribe : QueueProcessTrigger()

sealed class FeaturedRunAction(val runId: Int) {
    class MakeFeatured(
        runId: Int,
        val mediaType: MediaType,
    ) : FeaturedRunAction(runId) {
        val result: CompletableDeferred<AnalyticsCompanionRun?> = CompletableDeferred()
    }

    class MakeNotFeatured(runId: Int) : FeaturedRunAction(runId)
}

class QueueService {
    private val runs = mutableMapOf<Int, RunInfo>()
    private val removedRuns = mutableMapOf<Int, RunInfo>()
    private var featuredRun: FeaturedRunInfo? = null
    private val lastUpdateTime = mutableMapOf<Int, Duration>()

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
            isFirstSolvedRun -> FIRST_TO_SOLVE_WAIT_TIME
            else -> WAIT_TIME
        }

    suspend fun run(runsFlow: Flow<RunInfo>, contestInfoFlow: StateFlow<ContestInfo>) {
        val featuredRunsFlow = MutableSharedFlow<FeaturedRunAction>(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        DataBus.queueFeaturedRunsFlow.completeOrThrow(featuredRunsFlow)
        contestInfoFlow.filterNot { it.status == ContestStatus.BEFORE }.first()
        logger.info("Queue service is started")
        val firstEventTime = contestInfoFlow.value.currentContestTime
        val removerFlowTrigger = intervalFlow(1.seconds).map { Clean }
        val runsFlowTrigger = runsFlow.map { Run(it) }
        val subscriberFlowTrigger = subscriberFlow.map { Subscribe }
        val featuredFlowTrigger = featuredRunsFlow.map { Featured(it) }
        // it's important to have all side effects after merge, as part before merge will be executed concurrently
        merge(runsFlowTrigger, removerFlowTrigger, subscriberFlowTrigger, featuredFlowTrigger).collect { event ->
            when (event) {
                is Clean -> {
                    val currentTime = contestInfoFlow.value.currentContestTime
                    runs.values
                        .filter { currentTime >= lastUpdateTime[it.id]!! + it.timeInQueue }
                        .forEach { removeRun(it) }
                }
                is Run -> {
                    val run = event.run
                    removedRuns[run.id] = run
                    val currentTime =
                        contestInfoFlow.value.currentContestTime.takeIf { it != firstEventTime } ?: run.time
                    logger.debug("Receive run $run")
                    lastUpdateTime[run.id] = currentTime
                    if (run.isHidden) {
                        if (run.id in runs) {
                            removeRun(run)
                        }
                    } else {
                        if (run.id in runs || contestInfoFlow.value.currentContestTime <= currentTime + run.timeInQueue) {
                            modifyRun(run)
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
                    if (event.request is FeaturedRunAction.MakeFeatured) {
                        featuredRun?.makeNotFeatured()
                        featuredRun = FeaturedRunInfo(run.id, event.request.mediaType)
                        modifyRun(run)
                        lastUpdateTime[run.id] =
                            contestInfoFlow.value.currentContestTime.takeIf { it != firstEventTime } ?: run.time
                        event.request.result.complete(
                            AnalyticsCompanionRun(Clock.System.now() + FEATURED_WAIT_TIME, event.request.mediaType)
                        )
                    } else {
                        if (featuredRun?.runId == run.id) {
                            featuredRun = null
                            modifyRun(run, runId in runs)
                        }
                    }
                }
                is Subscribe -> {
                    resultFlow.emit(QueueSnapshotEvent(runs.values.sortedBy { it.id }))
                }
            }
            while (runs.size >= MAX_QUEUE_SIZE) {
                runs.values.asSequence()
                    .filterNot { it.isFirstSolvedRun || it.featuredRunMedia != null }
                    .minByOrNull { it.id }
                    ?.run { removeRun(this) }
            }
        }
    }

    companion object {
        val logger = getLogger(QueueService::class)

        private data class FeaturedRunInfo(val runId: Int, val mediaType: MediaType)

        private val WAIT_TIME = 1.minutes
        private val FIRST_TO_SOLVE_WAIT_TIME = 2.minutes
        private val FEATURED_WAIT_TIME = 1.minutes
        private const val MAX_QUEUE_SIZE = 15
    }
}
