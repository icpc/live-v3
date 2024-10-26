package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.*
import org.icpclive.cds.*
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.*
import kotlin.time.Duration


internal fun Flow<ContestUpdate>.processByTimeCut(
    timeCut : (ContestInfo?) -> Duration,
    process: (RunInfo) -> RunInfo
): Flow<ContestUpdate> = contestState().transform {
    suspend fun emit(info: RunInfo, cut: Duration) {
        println("${info.id} ${info.time} ${info.result} $cut ${it.lastEvent}")
        if (info.time > cut) {
            emit(RunUpdate(process(info)))
        } else {
            emit(RunUpdate(info))
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
                    .also { println("Updated runs: ${it.map { run -> run.id }}")}
                    .forEach { run -> emit(run, newCut) }
            }
        }
        is CommentaryMessagesUpdate -> emit(it.lastEvent)
    }
}.onEach { if (it is RunUpdate) println("${it.newInfo.id} ${it.newInfo.time} ${it.newInfo.result}") }

internal fun removeFrozenSubmissionsResults(flow: Flow<ContestUpdate>): Flow<ContestUpdate> =
    flow.processByTimeCut(
        timeCut = { it?.freezeTime ?: Duration.INFINITE },
    ) { it.copy(result = RunResult.InProgress(0.0)) }

internal fun removeAfterEndSubmissions(flow: Flow<ContestUpdate>): Flow<ContestUpdate> =
    flow.processByTimeCut(
        timeCut = { it?.contestLength ?: Duration.INFINITE },
    ) { it.copy(isHidden = true) }