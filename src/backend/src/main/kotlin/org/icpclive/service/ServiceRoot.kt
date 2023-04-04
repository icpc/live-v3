package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.config
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus
import org.icpclive.service.analytics.AnalyticsGenerator
import org.icpclive.util.reliableSharedFlow


fun CoroutineScope.launchServices(loader: ContestDataSource) {
    val runsDeferred = CompletableDeferred<Flow<RunInfo>>()
    val analyticsMessageFlowDeferred = CompletableDeferred<Flow<AnalyticsMessage>>()
    launch { loader.run(DataBus.contestInfoFlow, runsDeferred, analyticsMessageFlowDeferred) }
    launch { QueueService().run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
    launch {
        when (DataBus.contestInfoFlow.await().value.resultType) {
            ContestResultType.ICPC -> {
                launch { ScoreboardService(OptimismLevel.OPTIMISTIC).run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
                launch { ScoreboardService(OptimismLevel.PESSIMISTIC).run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
                launch { ScoreboardService(OptimismLevel.NORMAL).run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
                launch {
                    ICPCStatisticsService().run(
                        DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
                        DataBus.contestInfoFlow.await()
                    )
                }
                val generatedAnalyticsMessages = reliableSharedFlow<AnalyticsMessage>()
                launch {
                    config.analyticsTemplatesFile?.let {
                        AnalyticsGenerator(it).run(
                            generatedAnalyticsMessages,
                            DataBus.contestInfoFlow.await(),
                            runsDeferred.await(),
                            DataBus.getScoreboardEvents(OptimismLevel.NORMAL)
                        )
                    }
                }
                launch { AnalyticsService().run(merge(analyticsMessageFlowDeferred.await(), generatedAnalyticsMessages)) }
                launch {
                    val teamInterestingFlow = MutableStateFlow(emptyList<CurrentTeamState>())
                    val accentService = TeamSpotlightService(teamInteresting = teamInterestingFlow)
                    DataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)
                    DataBus.teamSpotlightFlow.completeOrThrow(accentService.getFlow())
                    accentService.run(
                        DataBus.contestInfoFlow.await(),
                        runsDeferred.await(),
                        DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
                        DataBus.teamInterestingScoreRequestFlow.await(),
                    )
                }
            }

            ContestResultType.IOI -> {
                launch { ScoreboardService(OptimismLevel.NORMAL).run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
                DataBus.setScoreboardEvents(OptimismLevel.OPTIMISTIC, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))
                DataBus.setScoreboardEvents(OptimismLevel.PESSIMISTIC, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))

                val generatedAnalyticsMessages = reliableSharedFlow<AnalyticsMessage>()
                launch {
                    config.analyticsTemplatesFile?.let {
                        AnalyticsGenerator(it).run(
                            generatedAnalyticsMessages,
                            DataBus.contestInfoFlow.await(),
                            runsDeferred.await(),
                            DataBus.getScoreboardEvents(OptimismLevel.NORMAL)
                        )
                    }
                }
                launch { AnalyticsService().run(merge(analyticsMessageFlowDeferred.await(), generatedAnalyticsMessages)) }
                launch {
                    generatedAnalyticsMessages.collect {
                        println("$it")
                    }
                }

                //DataBus.statisticFlow.completeOrThrow(emptyFlow())
                launch {
                    IOIStatisticsService().run(
                        DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
                        DataBus.contestInfoFlow.await()
                    )
                }

                launch {
                    val teamInterestingFlow = MutableStateFlow(emptyList<CurrentTeamState>())
                    val accentService = TeamSpotlightService(teamInteresting = teamInterestingFlow)
                    DataBus.teamInterestingFlow.completeOrThrow(teamInterestingFlow)
                    DataBus.teamSpotlightFlow.completeOrThrow(accentService.getFlow())
                    accentService.run(
                        DataBus.contestInfoFlow.await(),
                        runsDeferred.await(),
                        DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
                        DataBus.teamInterestingScoreRequestFlow.await(),
                    )
                }
            }
        }
    }
}
