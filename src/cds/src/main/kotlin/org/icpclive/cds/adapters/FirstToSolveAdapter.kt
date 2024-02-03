@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.*

private fun RunInfo.setFTS(value: Boolean) = when (result) {
    is RunResult.ICPC -> copy(result = result.copy(isFirstToSolveRun = value))
    is RunResult.IOI -> copy(result = result.copy(isFirstBestRun = value))
    is RunResult.InProgress -> this
}

public fun Flow<ContestUpdate>.addFirstToSolves(): Flow<ContestUpdate> = withGroupedRuns(
    selector = {

        when (it.result) {
            is RunResult.ICPC -> when {
                it.result.verdict.isAccepted && !it.isHidden -> it.problemId * 2
                else -> Int.MIN_VALUE
            }

            is RunResult.IOI -> when {
                it.result.isFirstBestTeamRun && !it.isHidden -> it.problemId * 2 + 1
                else -> Int.MIN_VALUE
            }

            is RunResult.InProgress -> Int.MIN_VALUE
        }
    },
    transformGroup = { k, runs, _, _ ->
        when {
            k == Int.MIN_VALUE -> runs
            k % 2 == 0 -> runs.mapIndexed { index, run ->
                run.setFTS(index == 0)
            }

            k % 2 != 0 -> {
                val bestRun = runs.maxByOrNull { (it.result as RunResult.IOI).scoreAfter }
                runs.map { it.setFTS(it == bestRun) }
            }

            else -> TODO()
        }
    }
).map { it.event }