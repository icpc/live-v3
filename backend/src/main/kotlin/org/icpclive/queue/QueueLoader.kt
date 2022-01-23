package org.icpclive.queue

import kotlinx.coroutines.delay
import org.icpclive.EventManager
import org.icpclive.api.*
import org.icpclive.events.*
import org.icpclive.events.RunInfo
import org.slf4j.*

class QueueLoader {
    suspend fun run() {
        val runs = mutableMapOf<Int, RunInfo>()
        var lastProcessedRunId = Int.MIN_VALUE
        while (true) {
            val newRuns = mutableListOf<RunInfo>()
            val modifiedRuns = mutableListOf<RunInfo>()

            for (run in EventsLoader.getInstance().contestData.runs.sortedBy { it.id }) {
                if (run.id > lastProcessedRunId) {
                    runs[run.id] = run
                    lastProcessedRunId = run.id
                    newRuns.add(run)
                } else {
                    val oldRun = runs[run.id] ?: continue
                    if (oldRun.lastUpdateTime != run.lastUpdateTime) {
                        runs[run.id] = run
                        modifiedRuns.add(run)
                    }
                }
            }
            val removedRuns = if (runs.size >= 20) {
                runs.values.sortedByDescending { it.id }.drop(20).toMutableList()
            } else {
                mutableListOf()
            }
            for (run in removedRuns) {
                runs.remove(run.id)
            }
            val ignoredRuns = removedRuns.intersect(newRuns.toSet())
            for (run in newRuns) {
                if (run in ignoredRuns) continue
                EventManager.processEvent(AddRunToQueueEvent(run.toApi()))
            }
            for (run in modifiedRuns) {
                if (run in ignoredRuns) continue
                EventManager.processEvent(ModifyRunInQueueEvent(run.toApi()))
            }
            for (run in removedRuns) {
                if (run in ignoredRuns) continue
                EventManager.processEvent(RemoveRunFromQueueEvent(run.toApi()))
            }
            delay(1000)
        }
    }
    companion object {
        val logger: Logger = LoggerFactory.getLogger(QueueLoader::class.java)
    }
}