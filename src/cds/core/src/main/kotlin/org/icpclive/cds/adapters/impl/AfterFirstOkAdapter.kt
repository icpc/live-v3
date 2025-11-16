package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.utils.withGroupedRuns

internal fun markSubmissionAfterFirstOk(flow: Flow<ContestUpdate>): Flow<ContestUpdate> = flow.withGroupedRuns(
    selector = { it.problemId to it.teamId },
    needUpdateGroup = { _, _, _ -> false },
    transformGroup = transform@{ _, _, runs, contestInfo ->
        if (contestInfo?.resultType != ContestResultType.ICPC) return@transform runs

        val bestIndex = runs.indexOfFirst { (it.result as? RunResult.ICPC)?.verdict?.isAccepted == true }
        if (bestIndex == -1 || bestIndex == runs.lastIndex) return@transform runs

        return@transform runs.take(bestIndex + 1) + runs.drop(bestIndex + 1).map {
            it.copy(result = when (it.result) {
                is RunResult.ICPC -> it.result.copy(isAfterFirstOk = true)
                is RunResult.InProgress -> it.result.copy(isAfterFirstOk = true)
                else -> it.result
            })
        }
    }
).map { it.event }