package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.utils.withGroupedRuns

private fun RunInfo.setFTS(value: Boolean) = when (result) {
    is RunResult.ICPC -> copy(result = result.copy(isFirstToSolveRun = value))
    is RunResult.IOI -> copy(result = result.copy(isFirstBestRun = value))
    is RunResult.InProgress -> this
}

private sealed class RunType {
    data class ICPCBest(val problemId: ProblemId) : RunType()
    data class IOIBest(val problemId: ProblemId) : RunType()
    data object NotBest : RunType()
}

internal fun addFirstToSolves(flow: Flow<ContestUpdate>): Flow<ContestUpdate> = flow.withGroupedRuns(
    selector = {
        when (it.result) {
            is RunResult.ICPC -> when {
                it.result.verdict.isAccepted && !it.isHidden -> RunType.ICPCBest(it.problemId)
                else -> RunType.NotBest
            }

            is RunResult.IOI -> when {
                it.result.isFirstBestTeamRun && !it.isHidden -> RunType.IOIBest(it.problemId)
                else -> RunType.NotBest
            }

            is RunResult.InProgress -> RunType.NotBest
        }
    },
    needUpdateGroup = { new, old, _ ->
        new.problems.any {
            it.value.ftsMode != old?.problems?.get(it.key)?.ftsMode
        } || new.awardsSettings.firstToSolveProblems != old?.awardsSettings?.firstToSolveProblems
    },
    transformGroup = transform@{ k, _, runs, info ->
        if (runs.isEmpty()) return@transform runs
        val bestRunId = when (val ftsMode = info?.problems?.get(runs[0].problemId)?.ftsMode) {
            is FtsMode.Hidden, null -> null
            is FtsMode.Custom -> ftsMode.runId
            is FtsMode.Auto -> {
                when (k) {
                    is RunType.NotBest -> null
                    is RunType.ICPCBest -> runs.firstOrNull()
                    is RunType.IOIBest -> runs.maxByOrNull { (it.result as RunResult.IOI).scoreAfter }
                }?.id.takeIf { info.awardsSettings.firstToSolveProblems }
            }
        }
        if (bestRunId == null) {
            runs
        } else {
            runs.map { if (it.id == bestRunId) it.setFTS(true) else it }
        }
    }
).map { it.event }