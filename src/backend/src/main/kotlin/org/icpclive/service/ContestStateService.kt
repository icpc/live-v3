package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.data.DataBus
import org.icpclive.util.completeOrThrow

class ContestStateService : Service {
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            DataBus.contestStateFlow.completeOrThrow(flow.map { it.state }.stateIn(this))
        }
    }
}