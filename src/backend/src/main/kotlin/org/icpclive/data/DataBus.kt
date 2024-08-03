package org.icpclive.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonObject
import org.icpclive.api.*
import org.icpclive.cds.api.*
import org.icpclive.service.AnalyticsAction
import org.icpclive.service.FeaturedRunAction

/**
 * Everything published here should be immutable, to allow secure work from many threads
 */
object DataBus {
    val contestStateFlow = CompletableDeferred<StateFlow<ContestState>>()
    val mainScreenFlow = CompletableDeferred<Flow<MainScreenEvent>>()
    val queueFlow = CompletableDeferred<Flow<QueueEvent>>()
    val externalRunsFlow = CompletableDeferred<Flow<Map<RunId, ExternalRunInfo>>>()
    val timelineFlow = CompletableDeferred<Flow<Map<TeamId, List<TimeLineRunInfo>>>>()

    // flow of run ids that need to be braking news
    val queueFeaturedRunsFlow = CompletableDeferred<FlowCollector<FeaturedRunAction>>()
    val tickerFlow = CompletableDeferred<Flow<TickerEvent>>()
    private val scoreboardDiffs = Array(OptimismLevel.entries.size) { CompletableDeferred<Flow<ScoreboardDiff>>() }
    val statisticFlow = CompletableDeferred<Flow<SolutionsStatistic>>()
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
    val teamSpotlightFlow = CompletableDeferred<Flow<KeyTeam>>()
    val teamInterestingScoreRequestFlow = CompletableDeferred<Flow<AddTeamScoreRequest>>()
    val teamInterestingFlow = CompletableDeferred<Flow<List<CurrentTeamState>>>()
    val socialEvents = CompletableDeferred<Flow<SocialEvent>>()
    val visualConfigFlow = CompletableDeferred<StateFlow<JsonObject>>()

    fun setScoreboardDiffs(level: OptimismLevel, flow: Flow<ScoreboardDiff>) {
        scoreboardDiffs[level.ordinal].complete(flow)
    }
    suspend fun getScoreboardDiffs(level: OptimismLevel) : Flow<ScoreboardDiff> = scoreboardDiffs[level.ordinal].await()
}

suspend fun DataBus.currentContestInfo() = currentContestInfoFlow().first()
suspend fun DataBus.currentContestInfoFlow() = contestStateFlow.await().mapNotNull { it.infoAfterEvent }.distinctUntilChanged()