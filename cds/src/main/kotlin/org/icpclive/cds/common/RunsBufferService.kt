package org.icpclive.cds.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import org.icpclive.api.RunInfo
import org.icpclive.common.util.getLogger
import org.icpclive.common.util.reliableSharedFlow

class RunsBufferService(
    private val runsStorageUpdates: Flow<List<RunInfo>>,
    private val runsFlowDeferred: CompletableDeferred<Flow<RunInfo>>
) {
    private val storage = mutableMapOf<Int, RunInfo>()

    suspend fun run() {
        val runsFlow = reliableSharedFlow<RunInfo>()
        runsFlowDeferred.complete(runsFlow)
        runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id] != run) {
                    log.debug("Run ${run.id} changed")
                    storage[run.id] = run
                    runsFlow.emit(run)
                }
            }
        }
    }

    companion object {
        private val log = getLogger(RunsBufferService::class)
    }
}