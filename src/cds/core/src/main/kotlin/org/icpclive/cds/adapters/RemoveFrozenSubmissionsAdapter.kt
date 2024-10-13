package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import kotlin.time.Duration


internal fun Flow<ContestUpdate>.processByTimeCut(
    timeCut : (ContestInfo?) -> Duration,
    process: (RunInfo) -> RunInfo
): Flow<ContestUpdate> = contestState().transform {
    suspend fun emit(info: RunInfo, cut: Duration) {
        if (info.time > cut) {
            emit(RunUpdate(process(info)))
        } else {
            emit(it.lastEvent)
        }
    }
    when (it.lastEvent) {
        is RunUpdate -> {
            emit(it.lastEvent.newInfo, timeCut(it.infoAfterEvent))
        }

        is InfoUpdate -> {
            emit(it.lastEvent)
            val oldCut = timeCut(it.infoBeforeEvent)
            val newCut = timeCut(it.infoAfterEvent)
            if (oldCut != newCut) {
                it.runsAfterEvent.values
                    .filter { run -> (run.time > oldCut) != (run.time > newCut) }
                    .forEach { run -> emit(run, newCut) }
            }
        }
        is CommentaryMessagesUpdate -> emit(it.lastEvent)
    }
}

public fun Flow<ContestUpdate>.removeFrozenSubmissionsResults(): Flow<ContestUpdate> =
    processByTimeCut(
        timeCut = { it?.freezeTime ?: Duration.INFINITE },
    ) { it.copy(result = RunResult.InProgress(0.0)) }

public fun Flow<ContestUpdate>.removeAfterEndSubmissions(): Flow<ContestUpdate> =
    processByTimeCut(
        timeCut = { it?.contestLength ?: Duration.INFINITE },
    ) { it.copy(isHidden = true) }