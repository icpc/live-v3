package org.icpclive.background

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import org.icpclive.DataBus
import org.icpclive.api.AddRunToQueueEvent
import org.icpclive.api.ModifyRunInQueueEvent
import org.icpclive.api.RemoveRunFromQueueEvent
import org.icpclive.api.RunInfo
import org.icpclive.cds.EventsLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueueProcessor {

    private val RunInfo.toOldAtTime get() = lastUpdateTime + if (isFirstSolvedRun) FIRST_TO_SOLVE_WAIT_TIME else WAIT_TIME

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun run() {
        val runs = mutableMapOf<Int, RunInfo>()
        val seenRunsSet = mutableSetOf<Int>()
        val ticker = flow {
            while (true) {
                emit(null)
                delay(1000)
            }
        }
        merge(DataBus.runsUpdates, ticker).collect { run ->
            val currentTime = DataBus.contestInfoFlow.value.currentContestTimeMs
            if (run != null) {
                logger.debug("Receive run $run")
                if (run.toOldAtTime > currentTime) {
                    if (run.id !in seenRunsSet || (run.isFirstSolvedRun && run.id !in runs)) {
                        runs[run.id] = run
                        seenRunsSet.add(run.id)
                        DataBus.queueEvents.emit(AddRunToQueueEvent(run))
                    } else if (run.id in runs) {
                        runs[run.id] = run
                        DataBus.queueEvents.emit(ModifyRunInQueueEvent(run))
                    }
                } else {
                    logger.info("Ignore run ${run.id} in queue as too old (currentTime = ${currentTime}, run.time = ${run.lastUpdateTime}, diff = ${currentTime - run.lastUpdateTime}")
                }
            }

            suspend fun removeRun(run: RunInfo) {
                runs.remove(run.id)
                DataBus.queueEvents.emit(RemoveRunFromQueueEvent(run))
            }

            runs.values.filter { currentTime >= it.toOldAtTime }.forEach { removeRun(it) }

            if (runs.size >= MAX_QUEUE_SIZE) {
                val runToRemove = runs.values.filterNot { it.isFirstSolvedRun }.minByOrNull { it.id }
                if (runToRemove != null) {
                    removeRun(runToRemove)
                }
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(QueueProcessor::class.java)

        private const val WAIT_TIME = 60000L
        private const val FIRST_TO_SOLVE_WAIT_TIME = 120000L
        private const val MAX_QUEUE_SIZE = 15

    }
}