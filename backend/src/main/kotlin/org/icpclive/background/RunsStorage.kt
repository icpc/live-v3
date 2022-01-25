package org.icpclive.background

import kotlinx.coroutines.flow.collect
import org.icpclive.DataBus
import org.icpclive.api.RunInfo as ApiRunInfo

object RunsStorage {
    private val storage = mutableMapOf<Int, ApiRunInfo>()

    suspend fun run() {
        DataBus.runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id] != run) {
                    storage[run.id] = run
                    DataBus.runsUpdates.emit(run)
                }
            }
        }
    }
}