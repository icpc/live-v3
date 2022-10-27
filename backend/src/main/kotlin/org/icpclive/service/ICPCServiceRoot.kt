package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.util.completeOrThrow
import org.icpclive.data.DataBus


fun CoroutineScope.launchICPCServices(loader: ContestDataSource) {
    val runsDeferred = CompletableDeferred<Flow<RunInfo>>()
    val analyticsMessageFlowDeferred = CompletableDeferred<Flow<AnalyticsMessage>>()
    launch { loader.run(DataBus.contestInfoFlow, runsDeferred, analyticsMessageFlowDeferred) }
    launch { QueueService().run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
    launch { ICPCNormalScoreboardService().run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
    launch { ICPCOptimisticScoreboardService().run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
    launch { ICPCPessimisticScoreboardService().run(runsDeferred.await(), DataBus.contestInfoFlow.await()) }
    launch {
        StatisticsService().run(
            DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
            DataBus.contestInfoFlow.await()
        )
    }
    launch { AnalyticsService().run(analyticsMessageFlowDeferred.await()) }
    launch {
        val teamInterestingFlow = MutableStateFlow(emptyList<TeamState>())
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
