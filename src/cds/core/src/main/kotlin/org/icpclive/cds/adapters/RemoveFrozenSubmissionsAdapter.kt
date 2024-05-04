package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.*
import org.icpclive.cds.api.ContestState
import org.icpclive.cds.api.RunResult
import kotlin.time.Duration


public fun Flow<ContestState>.removeFrozenSubmissions(): Flow<ContestUpdate> = transform {
    when (it.lastEvent) {
        is RunUpdate -> {
            if (it.infoBeforeEvent != null && it.lastEvent.newInfo.time >= it.infoBeforeEvent.freezeTime) {
                emit(
                    RunUpdate(
                        it.lastEvent.newInfo.copy(result = RunResult.InProgress(0.0))
                    )
                )
            } else {
                emit(it.lastEvent)
            }
        }

        is InfoUpdate -> {
            emit(it.lastEvent)
            if (it.lastEvent.newInfo.freezeTime != it.infoBeforeEvent?.freezeTime) {
                val newFreeze = it.lastEvent.newInfo.freezeTime
                val oldFreeze = it.infoBeforeEvent?.freezeTime ?: Duration.INFINITE
                it.runsAfterEvent.values.filter { run ->
                    (run.time < newFreeze) != (run.time < oldFreeze)
                }.forEach { run ->
                    emit(
                        RunUpdate(
                            if (run.time >= newFreeze) {
                                run.copy(result = RunResult.InProgress(0.0))
                            } else {
                                run
                            }
                        )
                    )
                }
            }
        }

        is AnalyticsUpdate -> emit(it.lastEvent)
    }
}