package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.getLogger
import org.icpclive.utils.tickerFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private sealed class QueueProcessTrigger
private object Clean : QueueProcessTrigger()
private class Run(val run: RunInfo) : QueueProcessTrigger()
private class MakeFeatured(val request: MakeRunFeaturedRequest) : QueueProcessTrigger()
private class MakeNotFeatured(val runId: Int) : QueueProcessTrigger()
private object Subscribe : QueueProcessTrigger()

data class MakeRunFeaturedRequestResult(
    val expirationTime: Instant,
)

class MakeRunFeaturedRequest(
    val runId: Int,
    val result: CompletableDeferred<MakeRunFeaturedRequestResult> = CompletableDeferred()
)


class QueueService {
    private val runs = mutableMapOf<Int, RunInfo>()
    private val removedRuns = mutableMapOf<Int, RunInfo>()
    private val featuredRuns = mutableSetOf<Int>()
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

    private suspend fun modifyRun(rawRun: RunInfo) {
        val isFeatured = rawRun.id in featuredRuns
        val run = rawRun.takeIf { it.isFeaturedRun == isFeatured } ?: rawRun.copy(isFeaturedRun = isFeatured)
        resultFlow.emit(if (run.id in runs) ModifyRunInQueueEvent(run) else AddRunToQueueEvent(run))
        runs[run.id] = run
    }

    private suspend fun removeRun(run: RunInfo) {
        runs.remove(run.id)
        featuredRuns.remove(run.id)
        removedRuns[run.id] = run
        resultFlow.emit(RemoveRunFromQueueEvent(run))
    }

    private val RunInfo.timeInQueue
        get() = when {
            isFeaturedRun -> FEATURED_WAIT_TIME
            isFirstSolvedRun -> FIRST_TO_SOLVE_WAIT_TIME
            else -> WAIT_TIME
        }

    suspend fun run(runsFlow: Flow<RunInfo>, contestInfoFlow: StateFlow<ContestInfo>) {
        val featuredRunsFlow = MutableSharedFlow<MakeRunFeaturedRequest>(
            extraBufferCapacity = 100,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        DataBus.queueFeaturedRunsFlow.completeOrThrow(featuredRunsFlow)
        contestInfoFlow.filterNot { it.status == ContestStatus.BEFORE }.first()
        logger.info("Queue service is started")
        val firstEventTime = contestInfoFlow.value.currentContestTime
        val removerFlowTrigger = tickerFlow(1.seconds).map { Clean }
        val runsFlowTrigger = runsFlow.map { Run(it) }
        val subscriberFlowTrigger = subscriberFlow.map { Subscribe }
        val makeFeaturedFlowTrigger = featuredRunsFlow.map { MakeFeatured(it) }
        // it's important to have all side effects after merge, as part before merge will be executed concurrently
        merge(runsFlowTrigger, removerFlowTrigger, subscriberFlowTrigger, makeFeaturedFlowTrigger).collect { event ->
            when (event) {
                is Clean -> {
                    val currentTime = contestInfoFlow.value.currentContestTime
                    runs.values
                        .filter { currentTime >= lastUpdateTime[it.id]!! + it.timeInQueue }.forEach { removeRun(it) }
                }
                is Run -> {
                    val run = event.run
                    val currentTime =
                        contestInfoFlow.value.currentContestTime.takeIf { it != firstEventTime } ?: run.time
                    logger.debug("Receive run $run")
                    lastUpdateTime[run.id] = currentTime
                    if (run.id in runs || contestInfoFlow.value.currentContestTime <= currentTime + run.timeInQueue) {
                        modifyRun(run)
                    }
                }
                is MakeFeatured -> {
                    val runId = event.request.runId
                    val run = runs[runId] ?: removedRuns[runId]
                    if (run == null) {
                        logger.warn("There is no run with id $runId for make it featured")
                        logger.info(runs.values.toString())
                        return@collect
                    }
                    featuredRuns += run.id
                    modifyRun(run)
                    val time = contestInfoFlow.value.currentContestTime.takeIf { it != firstEventTime } ?: run.time
                    lastUpdateTime[run.id] = time
                    event.request.result.complete(
                        MakeRunFeaturedRequestResult(Clock.System.now() + FEATURED_WAIT_TIME)
                    )
                }
                is MakeNotFeatured -> {
                    val run = runs[event.runId]
                    if (run == null) {
                        logger.warn("There is no run with id ${event.runId} for make it not featured")
                        return@collect
                    }
                    featuredRuns += run.id
                    modifyRun(run)
                }
                is Subscribe -> {
                    resultFlow.emit(QueueSnapshotEvent(runs.values.sortedBy { it.id }))
                }
            }
            while (runs.size >= MAX_QUEUE_SIZE) {
                runs.values.asSequence()
                    .filterNot { it.isFirstSolvedRun && it.isFeaturedRun }
                    .minByOrNull { it.id }
                    ?.run { removeRun(this) }
            }
        }
    }

    companion object {
        val logger = getLogger(QueueService::class)

        private val WAIT_TIME = 1.minutes
        private val FIRST_TO_SOLVE_WAIT_TIME = 2.minutes
        private val FEATURED_WAIT_TIME = 1.minutes
        private const val MAX_QUEUE_SIZE = 15
    }
}
