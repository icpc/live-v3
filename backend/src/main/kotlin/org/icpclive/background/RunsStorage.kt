package org.icpclive.background

import kotlinx.coroutines.flow.collect
import org.icpclive.DataBus
import org.icpclive.api.toApi
import org.icpclive.events.RunInfo

object RunsStorage {
    private val storage = mutableMapOf<Int, RunInfo>()

    suspend fun run() {
        DataBus.runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id]?.lastUpdateTime != run.lastUpdateTime) {
                    storage[run.id] = run
                    DataBus.runsUpdates.emit(run.toApi())
                }
            }
        }
    }
}