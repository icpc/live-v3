package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.utils.withGroupedRuns

private val logger by getLogger()

public fun Flow<ContestUpdate>.autoFinalize(progress: suspend (ContestStatus?, Int) -> Unit): Flow<ContestUpdate> = withGroupedRuns({ it.result !is RunResult.InProgress })
    .transformWhile {
        emit(it.event)
        val info = it.infoAfterEvent
        val status = info?.status
        if (status is ContestStatus.OVER && !info.cdsSupportsFinalization && it.runs[false].isNullOrEmpty()) {
            emit(InfoUpdate(it.infoAfterEvent!!.copy(
                status = ContestStatus.FINALIZED(
                    startedAt = status.startedAt,
                    finishedAt = status.finishedAt,
                    finalizedAt = status.finishedAt,
                    frozenAt = status.frozenAt
                )
            )))
            logger.info { "Contest finished. Finalizing. Found ${it.runs[true]?.size} total runs" }
            false
        } else {
            progress(info?.status, it.runs[false]?.size ?: 0)
            true
        }
    }

public fun Flow<ContestUpdate>.autoFinalize(): Flow<ContestUpdate> = autoFinalize { _, _ ->  }

public suspend fun Flow<ContestUpdate>.finalContestState(): ContestState = finalContestState { _, _ -> }


public suspend fun Flow<ContestUpdate>.finalContestState(progress: suspend (ContestStatus?, Int) -> Unit): ContestState = autoFinalize(progress)
    .contestState()
    .first { it.infoAfterEvent?.status is ContestStatus.FINALIZED }