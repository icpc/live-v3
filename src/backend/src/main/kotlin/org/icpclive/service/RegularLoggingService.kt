package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class RegularLoggingService : Service {
    private class Stats(
        val lastTime: Duration,
        val submissions: Int,
        val teams: Int
    )
    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        launch {
            var lastGc = TimeSource.Monotonic.markNow()
            val stats = flow.map {
                Stats(it.lastSubmissionTime, it.state.runsAfterEvent.size, it.state.infoAfterEvent?.teams?.size ?: 0)
            }.stateIn(this)
            while (true) {
                fun Long.mem() = "${(this / 1024 / 1024)}MB"
                delay(10.seconds)
                val runtime = Runtime.getRuntime()
                if (lastGc.elapsedNow() > 1.minutes) {
                    runtime.gc()
                    lastGc = TimeSource.Monotonic.markNow()
                }
                val curStats = stats.value
                log.info { "Processed contest up to ${curStats.lastTime}. It currently has ${curStats.teams} teams and ${curStats.submissions} submissions" }
                log.info {
                    "AllowedMemory: ${runtime.maxMemory().mem()}, UsedMem: ${(runtime.totalMemory() - runtime.freeMemory()).mem()}, AllocatedMemory: ${
                        runtime.totalMemory().mem()
                    }"
                }
            }
        }
    }
    companion object {
        val log by getLogger()
    }
}