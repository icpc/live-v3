package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.Config
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.service.analytics.AnalyticsGenerator


fun CoroutineScope.launchServices(loader: Flow<ContestUpdate>) {
    val loaded = loader.shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
    val runsFlow = loaded.filterIsInstance<RunUpdate>().map { it.newInfo }
    val analyticsFlow = loaded.filterIsInstance<AnalyticsUpdate>().map { it.message }
    launch {
        DataBus.contestInfoFlow.completeOrThrow(loaded.filterIsInstance<InfoUpdate>().map { it.newInfo }.stateIn(this))
    }
    launch { QueueService().run(runsFlow, DataBus.contestInfoFlow.await()) }
    launch {
        when (DataBus.contestInfoFlow.await().value.resultType) {
            ContestResultType.ICPC -> {
                launch { ScoreboardService(OptimismLevel.OPTIMISTIC).run(loaded) }
                launch { ScoreboardService(OptimismLevel.PESSIMISTIC).run(loaded) }
                launch { ScoreboardService(OptimismLevel.NORMAL).run(loaded) }
                launch { ICPCStatisticsService().run(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
            }

            ContestResultType.IOI -> {
                launch { ScoreboardService(OptimismLevel.NORMAL).run(loaded) }
                DataBus.setScoreboardEvents(OptimismLevel.OPTIMISTIC, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))
                DataBus.setScoreboardEvents(OptimismLevel.PESSIMISTIC, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))

                launch { IOIStatisticsService().run(DataBus.getScoreboardEvents(OptimismLevel.NORMAL)) }
            }
        }
        val generatedAnalyticsMessages = Config.analyticsTemplatesFile?.let {
            AnalyticsGenerator(it).getFlow(
                DataBus.contestInfoFlow.await(),
                runsFlow,
                DataBus.getScoreboardEvents(OptimismLevel.NORMAL)
            )
        } ?: emptyFlow()
        launch { AnalyticsService().run(merge(analyticsFlow, generatedAnalyticsMessages)) }
        launch {
            val teamInterestingFlow = MutableStateFlow(emptyList<CurrentTeamState>())
            val accentService = TeamSpotlightService(teamInteresting = teamInterestingFlow)
            DataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)
            DataBus.teamSpotlightFlow.completeOrThrow(accentService.getFlow())
            accentService.run(
                DataBus.contestInfoFlow.await(),
                runsFlow,
                DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
                DataBus.teamInterestingScoreRequestFlow.await(),
            )
        }
    }
}
