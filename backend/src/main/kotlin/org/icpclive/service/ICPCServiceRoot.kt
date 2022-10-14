package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.common.util.completeOrThrow
import org.icpclive.common.util.reliableSharedFlow
import org.icpclive.data.DataBus


fun CoroutineScope.launchICPCServices(loader: ContestDataSource) {
    val rawRunsDeferred = CompletableDeferred<Flow<RunInfo>>()
    val rawInfoFlowDeferred = CompletableDeferred<StateFlow<ContestInfo>>()
    val rawAnalyticsMessageFlowDeferred = CompletableDeferred<Flow<AnalyticsMessage>>()
    launch { loader.run(rawInfoFlowDeferred, rawRunsDeferred, rawAnalyticsMessageFlowDeferred) }
    val runsFlow = reliableSharedFlow<RunInfo>()
    launch { AdvancedPropertiesService().run(DataBus.advancedPropertiesFlow) }
    launch { ContestDataPostprocessingService().run(rawInfoFlowDeferred.await(), DataBus.advancedPropertiesFlow.await(), rawRunsDeferred.await(), DataBus.contestInfoFlow) }
    launch { QueueService().run(runsFlow, DataBus.contestInfoFlow.await()) }
    launch { ICPCNormalScoreboardService().run(runsFlow, DataBus.contestInfoFlow.await()) }
    launch { ICPCOptimisticScoreboardService().run(runsFlow, DataBus.contestInfoFlow.await()) }
    launch { ICPCPessimisticScoreboardService().run(runsFlow, DataBus.contestInfoFlow.await()) }
    launch { FirstToSolveService().run(rawRunsDeferred.await(), runsFlow) }
    launch { StatisticsService().run(DataBus.getScoreboardEvents(OptimismLevel.NORMAL), DataBus.contestInfoFlow.await()) }
    launch { AnalyticsService().run(rawAnalyticsMessageFlowDeferred.await()) }
    launch {
        val accentService = TeamSpotlightService()
        DataBus.teamSpotlightFlow.completeOrThrow(accentService.getFlow())
        accentService.run(DataBus.contestInfoFlow.await(), runsFlow, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))
    }
}
