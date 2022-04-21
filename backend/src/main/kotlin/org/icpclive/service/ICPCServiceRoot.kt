package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.OptimismLevel
import org.icpclive.data.DataBus


fun CoroutineScope.launchICPCServices(rawRuns: Flow<RunInfo>, infoFlow: StateFlow<ContestInfo>) {
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    launch { ContestDataOverridesService(infoFlow).run() }
    launch { QueueService(runsUpdates).run() }
    launch {
        val problemsNumber = infoFlow.filterNot { it.problems.isEmpty() }.first().problems.size
        launch { StatisticsService(problemsNumber, DataBus.getScoreboardEvents(OptimismLevel.NORMAL)).run() }
        launch { FirstToSolveService(problemsNumber, rawRuns, runsUpdates).run() }
        launch { ICPCNormalScoreboardService(problemsNumber, runsUpdates).run() }
        launch { ICPCOptimisticScoreboardService(problemsNumber, runsUpdates).run() }
        launch { ICPCPessimisticScoreboardService(problemsNumber, runsUpdates).run() }
    }
}