package org.icpclive.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import org.icpclive.api.*
import org.icpclive.cds.api.*
import org.icpclive.cds.util.DeferredFlow
import org.icpclive.service.*

/**
 * Everything published here should be immutable, to allow secure work from many threads
 */
class DataBus {
    val contestStateFlow = DeferredFlow<ContestState>()
    val contestInfoFlow = DeferredFlow<ContestInfo>()

    val mainScreenFlow = DeferredFlow<MainScreenEvent>()
    val queueFlow = DeferredFlow<QueueEvent>()
    val timelineFlow = DeferredFlow<Map<TeamId, List<TimeLineRunInfo>>>()

    // flow of run ids that need to be braking news
    val queueFeaturedRunsFlow = DeferredFlow<FeaturedRunAction>()
    val tickerFlow = DeferredFlow<TickerEvent>()
    private val scoreboardDiffs = Array(OptimismLevel.entries.size) { DeferredFlow<ScoreboardDiff>() }
    val statisticFlow = DeferredFlow<SolutionsStatistic>()
    val analyticsActionsFlow = DeferredFlow<AnalyticsAction>()
    val analyticsFlow = DeferredFlow<AnalyticsEvent>()
    val teamSpotlightFlow = DeferredFlow<KeyTeam>()
    val teamInterestingScoreRequestFlow = DeferredFlow<AddTeamScoreRequest>()
    val teamInterestingFlow = DeferredFlow<List<CurrentTeamState>>()
    val socialEvents = DeferredFlow<SocialEvent>()
    val visualConfigFlow = DeferredFlow<JsonObject>()

    val keylogService = CompletableDeferred<KeylogService>()

    fun setScoreboardDiffs(level: OptimismLevel, flow: Flow<ScoreboardDiff>) {
        scoreboardDiffs[level.ordinal].complete(flow)
    }
    fun getScoreboardDiffs(level: OptimismLevel) : Flow<ScoreboardDiff> = scoreboardDiffs[level.ordinal]
}
