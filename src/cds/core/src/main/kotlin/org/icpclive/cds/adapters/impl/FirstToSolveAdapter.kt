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
    transformGroup = { k, runs, _, info ->
        when (k) {
            is RunType.NotBest -> runs
            is RunType.ICPCBest -> runs.mapIndexed { index, run ->
                val ftsMode = info?.problems?.get(run.problemId)?.ftsMode
                when (ftsMode?.type) {
                    FtsMode.FtsModeType.HIDE -> {
                        return@mapIndexed run
                    }
                    FtsMode.FtsModeType.CUSTOM -> {
                        return@mapIndexed run.setFTS(ftsMode.runId == run.id && info.awardsSettings.firstToSolveProblems)
                    }
                    else ->  return@mapIndexed run.setFTS(index == 0 && info?.awardsSettings?.firstToSolveProblems != false)
                }

            }

            is RunType.IOIBest -> {
                val bestRun = runs.maxByOrNull { (it.result as RunResult.IOI).scoreAfter }
                runs.map {
                    val ftsMode = info?.problems?.get(it.problemId)?.ftsMode
                    when (ftsMode?.type) {
                        FtsMode.FtsModeType.HIDE -> {
                            return@map it
                        }
                        FtsMode.FtsModeType.CUSTOM -> {
                            return@map it.setFTS(ftsMode.runId == it.id && info.awardsSettings.firstToSolveProblems)
                        }
                        else -> return@map it.setFTS(it == bestRun && info?.awardsSettings?.firstToSolveProblems != false)
                    }
                }
            }
        }
    }
).map { it.event }