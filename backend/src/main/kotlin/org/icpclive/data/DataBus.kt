package org.icpclive.data

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.OptimismLevel
import org.icpclive.utils.LateInitFlow

/**
 * Everything published here should be immutable, to allow secure work from many threads
 *
 * So for now, we can't do flow of contestInfo, we need to refactor it first.
 * Only runs are published now, with copy of list to make this data immutable
 */
object DataBus {
    val contestInfoUpdates = MutableStateFlow(ContestInfo.EMPTY)
    val mainScreenFlow = LateInitFlow<MainScreenEvent>()
    val queueFlow = LateInitFlow<QueueEvent>()
    val tickerFlow = LateInitFlow<TickerEvent>()
    private val scoreboardFlow = Array(OptimismLevel.values().size) { LateInitFlow<Scoreboard>() }
    val statisticFlow = LateInitFlow<SolutionsStatistic>()

    fun setScoreboardEvents(level: OptimismLevel, flow: Flow<Scoreboard>) {
        scoreboardFlow[level.ordinal].set(flow)
    }

    suspend fun getScoreboardEvents(level: OptimismLevel) = scoreboardFlow[level.ordinal].get()


    @OptIn(FlowPreview::class)
    val allEvents
        get() = listOf(mainScreenFlow, queueFlow, tickerFlow)
            .map { flow { emit(it.get()) } }
            .merge()
            .flattenMerge(concurrency = Int.MAX_VALUE)
}
