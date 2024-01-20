@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.*
import kotlin.time.Duration


public fun Flow<ContestState>.removeFrozenSubmissions(): Flow<ContestUpdate> = transform {
    when (it.event) {
        is RunUpdate -> {
            if (it.infoBeforeEvent != null && it.event.newInfo.time >= it.infoBeforeEvent.freezeTime) {
                emit(RunUpdate(it.event.newInfo.copy(
                    result = null,
                    percentage = 0.0
                )))
            } else {
                emit(it.event)
            }
        }
        is InfoUpdate -> {
            emit(it.event)
            if (it.event.newInfo.freezeTime != it.infoBeforeEvent?.freezeTime) {
                val newFreeze = it.event.newInfo.freezeTime
                val oldFreeze = it.infoBeforeEvent?.freezeTime ?: Duration.INFINITE
                it.runs.values.filter { run ->
                    (run.time < newFreeze) != (run.time < oldFreeze)
                }.forEach { run ->
                    emit(RunUpdate(if (run.time >= newFreeze) {
                        run.copy(
                            result = null,
                            percentage = 0.0
                        )
                    } else {
                        run
                    }))
                }
            }
        }
        is AnalyticsUpdate -> emit(it.event)
    }
}