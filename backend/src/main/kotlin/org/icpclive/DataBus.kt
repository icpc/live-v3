package org.icpclive

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.OptimismLevel

/**
 * Everything published here should be immutable, to allow secure work from many threads
 *
 * So for now, we can't do flow of contestInfo, we need to refactor it first.
 * Only runs are published now, with copy of list to make this data immutable
 */
object DataBus {
    val contestInfoFlow = MutableStateFlow(ContestInfo.EMPTY)
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = 1000000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val mainScreenEventsHolder = MutableStateFlow(WidgetManager.widgetsFlow)
    val queueEventsFlowHolder = MutableStateFlow<Flow<QueueEvent>?>(null)
    val scoreboardFlowHolder = MutableStateFlow<Flow<Scoreboard>?>(null)
    val optimisticScoreboardFlowHolder = MutableStateFlow<Flow<Scoreboard>?>(null)
    val pessimisticScoreboardFlowHolder = MutableStateFlow<Flow<Scoreboard>?>(null)

    suspend fun mainScreenEvents() = mainScreenEventsHolder.filterNotNull().first()
    suspend fun queueEvents() = queueEventsFlowHolder.filterNotNull().first()
    fun setScoreboardEvents(level: OptimismLevel, flow: Flow<Scoreboard>) = when (level) {
        OptimismLevel.NORMAL -> scoreboardFlowHolder.value = flow
        OptimismLevel.OPTIMISTIC -> optimisticScoreboardFlowHolder.value = flow
        OptimismLevel.PESSIMISTIC -> pessimisticScoreboardFlowHolder.value = flow
    }
    suspend fun scoreboardEvents(level: OptimismLevel) = when (level) {
        OptimismLevel.NORMAL -> scoreboardFlowHolder.filterNotNull().first()
        OptimismLevel.OPTIMISTIC -> optimisticScoreboardFlowHolder.filterNotNull().first()
        OptimismLevel.PESSIMISTIC -> pessimisticScoreboardFlowHolder.filterNotNull().first()
    }

    @OptIn(FlowPreview::class)
    val allEvents
        get() = merge(
            mainScreenEventsHolder.take(1),
            queueEventsFlowHolder.filterNotNull().take(1)
        ).flattenMerge(concurrency = Int.MAX_VALUE)
}