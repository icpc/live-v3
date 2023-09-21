package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.*
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.util.getLogger

internal object AutoFinalize

public fun Flow<ContestUpdate>.autoFinalize(): Flow<ContestUpdate> = withGroupedRuns({ it.result != null })
    .transformWhile {
        emit(it.event)
        val info = it.infoAfterEvent
        if (info?.status == ContestStatus.OVER && !info.cdsSupportsFinalization && it.runs[false].isNullOrEmpty()) {
            emit(InfoUpdate(it.infoAfterEvent!!.copy(status = ContestStatus.FINALIZED)))
            getLogger(AutoFinalize::class).info("Contest finished. Finalizing.")
            false
        } else {
            true
        }
    }

public suspend fun Flow<ContestUpdate>.finalContestState(): ContestState = autoFinalize()
    .contestState()
    .first { it.infoAfterEvent?.status == ContestStatus.FINALIZED }