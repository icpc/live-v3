package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.api.RunInfo
import org.icpclive.cds.yandex.YandexEventLoader
import org.icpclive.utils.getLogger

class RunsBufferService(
    private val runsStorageUpdates: Flow<List<RunInfo>>,
    private val runsFlow: MutableSharedFlow<RunInfo>
) {
    private val storage = mutableMapOf<Int, RunInfo>()

    suspend fun run() {
        runsStorageUpdates.collect {
            for (run in it) {
                if (storage[run.id] != run) {
                    log.info("Run ${run.id} changed")
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