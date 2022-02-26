package org.icpclive.service

import kotlinx.coroutines.flow.*
import org.icpclive.api.RunInfo

class RunsBufferService(
    private val runsStorageUpdates: Flow<List<RunInfo>>,
    private val runsFlow: MutableSharedFlow<RunInfo>
    ) {
    private val storage = mutableMapOf<Int, RunInfo>()

    suspend fun run() {
        runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id] != run) {
                    storage[run.id] = run
                    runsFlow.emit(run)
                }
            }
        }
    }
}