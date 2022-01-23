package org.icpclive.queue

import kotlinx.coroutines.flow.collect
import org.icpclive.DataBus
import org.icpclive.api.*
import org.slf4j.*

class QueueLoader {
    suspend fun run() {
        val runs = mutableMapOf<Int, RunInfo>()
        val seenRunsSet = mutableSetOf<Int>()
        DataBus.runsUpdates.collect { run ->
            logger.info(run.toString())
            if (run.id !in seenRunsSet) {
                runs[run.id] = run
                seenRunsSet.add(run.id)
                DataBus.queueEvents.emit(AddRunToQueueEvent(run))
            } else if (run.id in runs){
                runs[run.id] = run
                DataBus.queueEvents.emit(ModifyRunInQueueEvent(run))
            }
            if (runs.size >= 20) {
                val runToRemove = runs.values.minByOrNull { it.id }!!
                runs.remove(runToRemove.id)
                DataBus.queueEvents.emit(RemoveRunFromQueueEvent(runToRemove))
            }
        }
    }
    companion object {
        val logger: Logger = LoggerFactory.getLogger(QueueLoader::class.java)
    }
}