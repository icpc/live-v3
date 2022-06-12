package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.api.OptimismLevel
import org.icpclive.data.DataBus


fun CoroutineScope.launchICPCServices(rawRuns: Flow<RunInfo>, infoFlow: StateFlow<ContestInfo>) {
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    launch { ContestDataOverridesService(infoFlow).run() }
    launch { QueueService(runsUpdates).run() }
    launch { ICPCNormalScoreboardService(runsUpdates).run() }
    launch { ICPCOptimisticScoreboardService(runsUpdates).run() }
    launch { ICPCPessimisticScoreboardService(runsUpdates).run() }
    launch { FirstToSolveService(rawRuns, runsUpdates).run() }
    launch {
        StatisticsService(
            DataBus.getScoreboardEvents(OptimismLevel.NORMAL),
            DataBus.contestInfoUpdates.await()
        ).launch(this)
    }
}