package org.icpclive.cds.adapters

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import kotlin.time.Duration

internal class RemoveFrozenSubmissionsAdapter(private val source: ContestDataSource) : ContestDataSource {
    sealed class Event
    class RunEvent(val run: RunInfo): Event()
    class FreezeTimeUpdateEvent(val freezeTime: Duration): Event()

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        val unprocessedRuns = CompletableDeferred<Flow<RunInfo>>()
        coroutineScope {
            launch { source.run(contestInfoDeferred, unprocessedRuns, analyticsMessagesDeferred) }
            val freezeTimeFlow = contestInfoDeferred.await().map { it.freezeTime }.distinctUntilChanged()
            var currentFreezeTime = freezeTimeFlow.first()
            val hiddenRunInfos = mutableMapOf<Int, RunInfo>()
            val sentRunInfos = mutableMapOf<Int, RunInfo>()
            val resultFlow = flow {
                suspend fun processRun(run: RunInfo) {
                    hiddenRunInfos.remove(run.id)
                    sentRunInfos.remove(run.id)
                    if (run.time >= currentFreezeTime) {
                        hiddenRunInfos[run.id] = run
                        emit(run.copy(result = null, percentage = 0.0))
                    } else {
                        sentRunInfos[run.id] = run
                        emit(run)
                    }
                }
                merge(
                    unprocessedRuns.await().map(::RunEvent),
                    freezeTimeFlow.map(::FreezeTimeUpdateEvent)
                ).collect { event ->
                    when (event) {
                        is RunEvent -> {
                            processRun(event.run)
                        }

                        is FreezeTimeUpdateEvent -> {
                            currentFreezeTime = event.freezeTime
                            val toRecheck = hiddenRunInfos.values.filter { it.time < currentFreezeTime } +
                                    sentRunInfos.values.filter { it.time >= currentFreezeTime }
                            for (run in toRecheck) {
                                processRun(run)
                            }
                        }
                    }
                }
            }.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
            runsDeferred.complete(resultFlow)
        }

    }
}