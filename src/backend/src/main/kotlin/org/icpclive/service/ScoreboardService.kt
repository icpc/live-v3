package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.stateGroupedByTeam
import org.icpclive.data.DataBus
import org.icpclive.scoreboard.calculateScoreboard
import org.icpclive.util.getLogger


class ScoreboardService(val optimismLevel: OptimismLevel) {

    suspend fun run(
        updates: Flow<ContestUpdate>,
    ) {
        logger.info("Scoreboard service for optimismLevel=${optimismLevel} started")
        coroutineScope {
            updates.stateGroupedByTeam()
                .calculateScoreboard(optimismLevel)
                .stateIn(this, SharingStarted.Eagerly, Scoreboard(emptyList()))
                .let { DataBus.setScoreboardEvents(optimismLevel, it) }
        }
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}


