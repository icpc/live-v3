package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.icpclive.api.RunInfo


fun CoroutineScope.launchICPCServices(problemsNumber:Int, rawRuns: Flow<RunInfo>) {
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = 1000000,
        onBufferOverflow = BufferOverflow.SUSPEND
    )
    launch { QueueService(runsUpdates).run() }
    launch { StatisticsService(problemsNumber, runsUpdates).run() }
    launch { FirstToSolveService(problemsNumber, rawRuns, runsUpdates).run() }
    launch { ICPCNormalScoreboardService(problemsNumber, runsUpdates).run() }
    launch { ICPCOptimisticScoreboardService(problemsNumber, runsUpdates).run() }
    launch { ICPCPessimisticScoreboardService(problemsNumber, runsUpdates).run() }
}