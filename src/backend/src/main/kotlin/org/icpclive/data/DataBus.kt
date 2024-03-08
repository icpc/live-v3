package org.icpclive.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.JsonObject
import org.icpclive.api.*
import org.icpclive.cds.adapters.ContestState
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ScoreboardAndContestInfo
import org.icpclive.util.completeOrThrow
import org.icpclive.service.AnalyticsAction
import org.icpclive.service.FeaturedRunAction

/**
 * Everything published here should be immutable, to allow secure work from many threads
 */
object DataBus {
    val contestInfoFlow = CompletableDeferred<StateFlow<ContestInfo>>()
    val contestStateFlow = CompletableDeferred<StateFlow<ContestState>>()
    val mainScreenFlow = CompletableDeferred<Flow<MainScreenEvent>>()
    val queueFlow = CompletableDeferred<Flow<QueueEvent>>()

    // flow of run ids that need to be braking news
    val queueFeaturedRunsFlow = CompletableDeferred<FlowCollector<FeaturedRunAction>>()
    val tickerFlow = CompletableDeferred<Flow<TickerEvent>>()
    private val legacyScoreboardFlow = Array(OptimismLevel.entries.size) { CompletableDeferred<Flow<LegacyScoreboard>>() }
    private val scoreboardFlow = Array(OptimismLevel.entries.size) { CompletableDeferred<Flow<ScoreboardAndContestInfo>>() }
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
    val teamSpotlightFlow = CompletableDeferred<Flow<KeyTeam>>()
    val teamInterestingScoreRequestFlow = CompletableDeferred<Flow<AddTeamScoreRequest>>()
    val teamInterestingFlow = CompletableDeferred<Flow<List<CurrentTeamState>>>()
    val socialEvents = CompletableDeferred<Flow<SocialEvent>>()
    val visualConfigFlow = CompletableDeferred<StateFlow<JsonObject>>()

    fun setScoreboardEvents(level: OptimismLevel, flow: Flow<ScoreboardAndContestInfo>) { scoreboardFlow[level.ordinal].completeOrThrow(flow) }
    suspend fun getScoreboardEvents(level: OptimismLevel) = scoreboardFlow[level.ordinal].await()
}
