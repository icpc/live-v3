package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.*
import org.icpclive.data.DataBus
import org.icpclive.util.getLogger

class ScoreboardService(vararg val optimismLevel: OptimismLevel) {
    suspend fun run(
        updates: Flow<ContestStateWithScoreboard>,
    ) {
        logger.info("Scoreboard service for levels ${optimismLevel.toList()} started")
        coroutineScope {
            val scoreboardFlow = updates.shareIn(this, SharingStarted.Eagerly, replay = 1)
            for (level in optimismLevel) {
                DataBus.setScoreboardDiffs(level, scoreboardFlow.withIndex().map { (index, it) -> it.toScoreboardDiff(snapshot = index == 0) })
                DataBus.setLegacyScoreboard(level, scoreboardFlow.map { it.toLegacyScoreboard() })
            }
        }
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}


