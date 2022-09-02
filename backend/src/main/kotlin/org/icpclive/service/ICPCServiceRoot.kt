package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.reliableSharedFlow
import kotlin.time.Duration.Companion.seconds


fun CoroutineScope.launchICPCServices(
    rawRuns: Flow<RunInfo>,
    rawInfoFlow: StateFlow<ContestInfo>,
    rawAnalyticsMessageFlow: SharedFlow<AnalyticsMessage> = MutableSharedFlow()
) {
    val runsFlow = reliableSharedFlow<RunInfo>()
    val advancedPropertiesFlow = MutableStateFlow(AdvancedProperties())
    val infoFlow = MutableStateFlow(rawInfoFlow.value)
    DataBus.advancedPropertiesFlow.completeOrThrow(advancedPropertiesFlow)
    DataBus.contestInfoFlow.completeOrThrow(infoFlow)
    launch { AdvancedPropertiesService().run(advancedPropertiesFlow) }
    launch { ContestDataPostprocessingService().run(rawInfoFlow, advancedPropertiesFlow, rawRuns, infoFlow) }
    launch { QueueService().run(runsFlow, infoFlow) }
    launch { ICPCNormalScoreboardService().run(runsFlow, infoFlow) }
    launch { ICPCOptimisticScoreboardService().run(runsFlow, infoFlow) }
    launch { ICPCPessimisticScoreboardService().run(runsFlow, infoFlow) }
    launch { FirstToSolveService().run(rawRuns, runsFlow) }
    launch { StatisticsService().run(DataBus.getScoreboardEvents(OptimismLevel.NORMAL), infoFlow) }
    launch { AnalyticsService().run(rawAnalyticsMessageFlow) }
    launch {
        val accentService = TeamSpotlightService(this)
        DataBus.autoSplitScreenTeams.completeOrThrow(accentService.getFlow(10.seconds, TeamSpotlightFlowSettings()))
        accentService.run(infoFlow, runsFlow, DataBus.getScoreboardEvents(OptimismLevel.NORMAL))
    }
}
