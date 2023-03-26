package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.*
import kotlin.time.Duration.Companion.minutes

private fun RunInfo.setICPC(value: Boolean) = copy(result = (result as? ICPCRunResult)?.copy(isFirstToSolveRun = value))
private fun RunInfo.setIOI(value: Boolean) = copy(result = (result as? IOIRunResult)?.copy(isFirstBestRun = value))

fun Flow<ContestUpdate>.addFirstToSolves() = withGroupedRuns(
    selector = {

        when (it.result) {
            is ICPCRunResult -> when {
                it.result.isAccepted && !it.isHidden -> it.problemId * 2
                else -> Int.MIN_VALUE
            }
            is IOIRunResult -> when {
                it.result.isFirstBestTeamRun && !it.isHidden -> it.problemId * 2 + 1
                else -> Int.MIN_VALUE
            }
            else -> Int.MIN_VALUE
        }
    },
    transformGroup = { k, runs, _ ->
        when {
            k == Int.MIN_VALUE -> runs
            k % 2 == 0 -> runs.mapIndexed { index, run ->
                run.setICPC(index == 0)
            }
            k % 2 != 0 -> {
                val bestRun = runs.maxByOrNull { (it.result as IOIRunResult).scoreAfter }
                runs.map { it.setIOI(it == bestRun) }
            }
            else -> TODO()
        }
    }
).map { it.event }