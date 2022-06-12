package org.icpclive.service

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
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
private object Subscribe : QueueProcessTrigger()


class QueueService(private val runsFlow: Flow<RunInfo>) {
    private val runs = mutableMapOf<Int, RunInfo>()
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

    private suspend fun removeRun(run: RunInfo) {
        runs.remove(run.id)
        resultFlow.emit(RemoveRunFromQueueEvent(run))
    }

    private val RunInfo.timeInQueue
        get() = if (isFirstSolvedRun) FIRST_TO_SOLVE_WAIT_TIME else WAIT_TIME


    suspend fun run() {
        val contestInfoFlow = DataBus.contestInfoUpdates.await()
        contestInfoFlow.filterNot { it.status == ContestStatus.BEFORE }.first()
        logger.info("Queue service is started")
        val firstEventTime = contestInfoFlow.value.currentContestTime
        val removerFlowTrigger = tickerFlow(1.seconds).map { Clean }
        val runsFlowTrigger = runsFlow.map { Run(it) }
        val subscriberFlowTrigger = subscriberFlow.map { Subscribe }
        // it's important to have all side effects after merge, as part before merge will be executed concurrently
        merge(runsFlowTrigger, removerFlowTrigger, subscriberFlowTrigger).collect { event ->
            when (event) {
                is Clean -> {
                    val currentTime = contestInfoFlow.value.currentContestTime
                    runs.values
                        .filter { currentTime >= lastUpdateTime[it.id]!! + it.timeInQueue }.forEach { removeRun(it) }
                }
                is Run -> {
                    val run = event.run
                    val currentTime = contestInfoFlow.value.currentContestTime.takeIf { it != firstEventTime } ?: run.time
                    logger.debug("Receive run $run")
                    lastUpdateTime[run.id] = currentTime
                    if (run.id in runs || contestInfoFlow.value.currentContestTime <= currentTime + run.timeInQueue) {
                        resultFlow.emit(if (run.id in runs) ModifyRunInQueueEvent(run) else AddRunToQueueEvent(run))
                        runs[run.id] = run
                    }
                }
                is Subscribe -> {
                    resultFlow.emit(QueueSnapshotEvent(runs.values.sortedBy { it.id }))
                }
            }
            while (runs.size >= MAX_QUEUE_SIZE) {
                runs.values.asSequence()
                    .filterNot { it.isFirstSolvedRun }
                    .minByOrNull { it.id }
                    ?.run { removeRun(this) }
            }
        }
    }

    companion object {
        val logger = getLogger(QueueService::class)

        private val WAIT_TIME = 1.minutes
        private val FIRST_TO_SOLVE_WAIT_TIME = 2.minutes
        private const val MAX_QUEUE_SIZE = 15
    }
}
