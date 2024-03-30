package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.data.DataBus
import org.icpclive.util.getLogger

class ScoreboardService(val optimismLevel: OptimismLevel) {
    suspend fun run(
        updates: Flow<ContestUpdate>,
    ) {
        logger.info("Scoreboard service for optimismLevel=${optimismLevel} started")
        coroutineScope {
            val scoreboardFlow = updates.calculateScoreboard(optimismLevel)
                .shareIn(this, SharingStarted.Eagerly, replay = Int.MAX_VALUE)
            DataBus.setScoreboardEvents(optimismLevel, scoreboardFlow)
        }
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}


