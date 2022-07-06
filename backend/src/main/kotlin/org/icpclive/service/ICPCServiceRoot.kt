package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.utils.completeOrThrow


fun CoroutineScope.launchICPCServices(
    rawRuns: Flow<RunInfo>,
    rawInfoFlow: StateFlow<ContestInfo>,
    analyticsEventFlow: SharedFlow<AnalyticsEvents> = MutableSharedFlow()
) {
    val runsFlow = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    val advancedPropertiesFlow = MutableStateFlow(AdvancedProperties())
    val infoFlow = MutableStateFlow(rawInfoFlow.value)
    DataBus.advancedPropertiesFlow.completeOrThrow(advancedPropertiesFlow)
    DataBus.contestInfoFlow.completeOrThrow(infoFlow)
    launch { AdvancedPropertiesService().run(advancedPropertiesFlow) }
    launch { ContestDataOverridesService().run(rawInfoFlow, advancedPropertiesFlow, infoFlow) }
    launch { QueueService().run(runsFlow, infoFlow) }
    launch { ICPCNormalScoreboardService().run(runsFlow, infoFlow, advancedPropertiesFlow) }
    launch { ICPCOptimisticScoreboardService().run(runsFlow, infoFlow, advancedPropertiesFlow) }
    launch { ICPCPessimisticScoreboardService().run(runsFlow, infoFlow, advancedPropertiesFlow) }
    launch { FirstToSolveService().run(rawRuns, runsFlow) }
    launch { StatisticsService().run(DataBus.getScoreboardEvents(OptimismLevel.NORMAL), infoFlow) }
    launch { DataBus.analyticsEventFlow.complete(analyticsEventFlow) }
}
