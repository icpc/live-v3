package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.icpclive.DataBus
import org.icpclive.api.RunInfo


fun CoroutineScope.launchICPCServices(problemsNumber:Int, rawRuns: Flow<RunInfo>) {
    launch { QueueService(DataBus.runsUpdates).run() }
    launch { StatisticsService(problemsNumber, DataBus.runsUpdates).run() }
    launch { FirstToSolveService(problemsNumber, rawRuns, DataBus.runsUpdates).run() }
    launch { ICPCNormalScoreboardService(problemsNumber, DataBus.runsUpdates).run() }
    launch { ICPCOptimisticScoreboardService(problemsNumber, DataBus.runsUpdates).run() }
    launch { ICPCPessimisticScoreboardService(problemsNumber, DataBus.runsUpdates).run() }
}