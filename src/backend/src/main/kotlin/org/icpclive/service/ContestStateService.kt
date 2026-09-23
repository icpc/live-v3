package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.completeOrThrow
import kotlinx.coroutines.CompletableDeferred
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.ContestState

class ContestStateService(
    private val contestStateFlow: CompletableDeferred<StateFlow<ContestState>>,
) : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            contestStateFlow.completeOrThrow(flow.map { it.state }.stateIn(this))
        }
    }
}