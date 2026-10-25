package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.ContestState
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.DeferredFlow

class ContestStateService(
    private val contestStateFlow: DeferredFlow<ContestState>,
    private val contestInfoFlow: DeferredFlow<ContestInfo>,
) : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            contestStateFlow.complete(flow.map { it.state }.stateIn(this))
        }
        launch {
            contestInfoFlow.complete(contestStateFlow.mapNotNull { it.infoAfterEvent }.stateIn(this))
        }
    }
}