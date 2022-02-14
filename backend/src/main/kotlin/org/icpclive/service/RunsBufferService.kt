package org.icpclive.service

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import org.icpclive.DataBus
import org.icpclive.api.RunInfo

class RunsBufferService(val runsStorageUpdates: SharedFlow<List<RunInfo>>) {
    private val storage = mutableMapOf<Int, RunInfo>()

    suspend fun run() {
        runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id] != run) {
                    storage[run.id] = run
                    DataBus.runsUpdates.emit(run)
                }
            }
        }
    }
}