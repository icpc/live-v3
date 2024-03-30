package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.Config
import org.icpclive.api.*
import org.icpclive.cds.*
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

    fun CoroutineScope.launchService(service: Service) = launch {
        with(service) {
            launch {
                runOn(normalScoreboardState)
            }
        }
    }

    launchService {
        launch {
            DataBus.contestStateFlow.completeOrThrow(it.map { it.state }.stateIn(this))
        }
    }
    launchService {
        launch {
            DataBus.contestInfoFlow.completeOrThrow(
                it.filter { it.state.lastEvent is InfoUpdate }
                    .mapNotNull { it.state.infoAfterEvent }
                    .stateIn(this)
            )
        }
    }
    launchService(QueueService())
    launchService(ScoreboardService())
    launchService(StatisticsService())
    launchService(AnalyticsService(AnalyticsGenerator(Config.analyticsTemplatesFile)))
    launchService(ExternalRunsService())

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
