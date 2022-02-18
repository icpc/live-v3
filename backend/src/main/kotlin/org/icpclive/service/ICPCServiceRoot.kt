package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.icpclive.DataBus
import org.icpclive.api.RunInfo


fun CoroutineScope.launchICPCServices(problemsNumber:Int, rawRuns: Flow<RunInfo>) {
    launch { FirstToSolveService(problemsNumber, rawRuns, DataBus.runsUpdates).run() }
    launch { ICPCNormalScoreboardService(problemsNumber, DataBus.runsUpdates, DataBus.scoreboardFlow).run() }
    launch { ICPCOptimisticScoreboardService(problemsNumber, DataBus.runsUpdates, DataBus.optimisticScoreboardFlow).run() }
    launch { ICPCPessimisticScoreboardService(problemsNumber, DataBus.runsUpdates, DataBus.pessimisticScoreboardFlow).run() }
}