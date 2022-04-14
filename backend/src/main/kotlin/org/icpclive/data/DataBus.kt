package org.icpclive.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
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
    val contestInfoUpdates = CompletableDeferred<StateFlow<ContestInfo>>()
    val mainScreenFlow = CompletableDeferred<Flow<MainScreenEvent>>()
    val queueFlow = CompletableDeferred<Flow<QueueEvent>>()
    val tickerFlow = CompletableDeferred<Flow<TickerEvent>>()
    private val scoreboardFlow = Array(OptimismLevel.values().size) { CompletableDeferred<Flow<Scoreboard>>() }
    val statisticFlow = CompletableDeferred<Flow<SolutionsStatistic>>()

    fun setScoreboardEvents(level: OptimismLevel, flow: Flow<Scoreboard>) {
        scoreboardFlow[level.ordinal].complete(flow)
    }

    suspend fun getScoreboardEvents(level: OptimismLevel) = scoreboardFlow[level.ordinal].await()


    @OptIn(FlowPreview::class)
    val allEvents
        get() = listOf(mainScreenFlow, queueFlow, tickerFlow)
            .map { flow { emit(it.await()) } }
            .merge()
            .flattenMerge(concurrency = Int.MAX_VALUE)
}
