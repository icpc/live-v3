package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.service.analytics.AnalyticsGenerator

fun CoroutineScope.launchServices(loader: Flow<ContestUpdate>) {
    val loaded = loader.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
    val normalScoreboardState = loaded.calculateScoreboard(OptimismLevel.NORMAL).shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)

    val runsFlow = loaded.filterIsInstance<RunUpdate>().map { it.newInfo }
    launch {
        DataBus.contestStateFlow.completeOrThrow(
            normalScoreboardState.map { it.state }.stateIn(this)
        )
    }
    launch {
        DataBus.contestInfoFlow.completeOrThrow(
            normalScoreboardState.filter { it.state.lastEvent is InfoUpdate }
                .mapNotNull { it.state.infoAfterEvent }
                .stateIn(this)
        )
    }
    launch { QueueService().run(normalScoreboardState) }
    launch {
        when (DataBus.contestInfoFlow.await().value.resultType) {
            ContestResultType.ICPC -> {
                launch { ScoreboardService(OptimismLevel.OPTIMISTIC).run(normalScoreboardState) }
                launch { ScoreboardService(OptimismLevel.PESSIMISTIC).run(normalScoreboardState.map { it.state.lastEvent }.calculateScoreboard(OptimismLevel.PESSIMISTIC)) }
                launch { ScoreboardService(OptimismLevel.NORMAL).run(normalScoreboardState.map { it.state.lastEvent }.calculateScoreboard(OptimismLevel.OPTIMISTIC)) }
                launch { ICPCStatisticsService().run(normalScoreboardState) }
            }

            ContestResultType.IOI -> {
                launch { ScoreboardService(OptimismLevel.NORMAL, OptimismLevel.OPTIMISTIC, OptimismLevel.PESSIMISTIC).run(normalScoreboardState) }
                launch { IOIStatisticsService().run(normalScoreboardState) }
            }
        }
        val generatedAnalyticsMessages = AnalyticsGenerator(Config.analyticsTemplatesFile).getFlow(normalScoreboardState)
        launch { AnalyticsService().run(generatedAnalyticsMessages) }
        launch { ExternalRunsService().run(normalScoreboardState) }
        launch {
            val teamInterestingFlow = MutableStateFlow(emptyList<CurrentTeamState>())
            val accentService = TeamSpotlightService(teamInteresting = teamInterestingFlow)
            DataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)
            DataBus.teamSpotlightFlow.completeOrThrow(accentService.getFlow())
            accentService.run(
                DataBus.contestInfoFlow.await(),
                runsFlow,
                normalScoreboardState,
                DataBus.teamInterestingScoreRequestFlow.await(),
            )
        }
    }
}
