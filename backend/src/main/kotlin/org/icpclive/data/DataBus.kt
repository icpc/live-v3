package org.icpclive.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.service.AnalyticsAction
import org.icpclive.service.MakeRunFeaturedRequest
import org.icpclive.utils.completeOrThrow

/**
 * Everything published here should be immutable, to allow secure work from many threads
 *
 * So for now, we can't do flow of contestInfo, we need to refactor it first.
 * Only runs are published now, with copy of list to make this data immutable
 */
object DataBus {
    val contestInfoFlow = CompletableDeferred<Flow<ContestInfo>>()
    val mainScreenFlow = CompletableDeferred<Flow<MainScreenEvent>>()
    val queueFlow = CompletableDeferred<Flow<QueueEvent>>()
    // flow of run ids that need to be braking news
    val queueFeaturedRunsFlow = CompletableDeferred<FlowCollector<MakeRunFeaturedRequest>>()
    val tickerFlow = CompletableDeferred<Flow<TickerEvent>>()
    private val scoreboardFlow = Array(OptimismLevel.values().size) { CompletableDeferred<Flow<Scoreboard>>() }
    val statisticFlow = CompletableDeferred<Flow<SolutionsStatistic>>()
    val advancedPropertiesFlow = CompletableDeferred<Flow<AdvancedProperties>>()
    val analyticsActionsFlow = CompletableDeferred<Flow<AnalyticsAction>>()
    val analyticsFlow = CompletableDeferred<Flow<AnalyticsEvent>>()
    val loggerFlow = MutableSharedFlow<String>(
        replay = 500,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val adminActionsFlow = MutableSharedFlow<String>(
        replay = 500,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun setScoreboardEvents(level: OptimismLevel, flow: Flow<Scoreboard>) {
        scoreboardFlow[level.ordinal].completeOrThrow(flow)
    }

    suspend fun getScoreboardEvents(level: OptimismLevel) = scoreboardFlow[level.ordinal].await()

    val allEvents
        get() = listOf(mainScreenFlow, queueFlow, tickerFlow)
            .map { flow { emit(it.await()) } }
            .merge()
            .flattenMerge(concurrency = Int.MAX_VALUE)
}
