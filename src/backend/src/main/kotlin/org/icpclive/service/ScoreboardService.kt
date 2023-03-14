package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.scoreboard.getScoreboardCalculator
import org.icpclive.util.getLogger


class ScoreboardService(val optimismLevel: OptimismLevel) {

    suspend fun run(
        runsFlow: Flow<RunInfo>,
        contestInfoFlow: Flow<ContestInfo>,
    ) {
        logger.info("Scoreboard service for optimismLevel=${optimismLevel} started")
        coroutineScope {
            var info: ContestInfo? = null
            val runs = mutableMapOf<Int, RunInfo>()
            // We need mutex, as conflate runs part of flow before and after it in different coroutines
            // So we make part before as lightweight as possible, and just drops intermediate values
            val mutex = Mutex()
            merge(runsFlow, contestInfoFlow).mapNotNull { update ->
                when (update) {
                    is RunInfo -> {
                        mutex.withLock {
                            val oldRun = runs[update.id]
                            runs[update.id] = update
                            if (oldRun != null && oldRun.result == update.result) {
                                return@mapNotNull null
                            }
                        }
                    }

                    is ContestInfo -> {
                        info = update
                    }
                }
                // It would be nice to copy runs here to avoid mutex, but it is too slow
                info
            }
                .conflate()
                .map { getScoreboardCalculator(it, optimismLevel).getScoreboard(it, mutex.withLock { runs.values.toList() }) }
                .stateIn(this)
                .let { DataBus.setScoreboardEvents(optimismLevel, it) }
        }
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}


