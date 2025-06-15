package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.*
import org.icpclive.data.DataBus
import org.icpclive.cds.util.getLogger

class ScoreboardService : Service {
    private fun setUp(level: OptimismLevel, flow: Flow<ContestStateWithScoreboard>) {
        DataBus.setScoreboardDiffs(level, flow.withIndex().filter { it.index == 0 || it.value.isAffectingScoreboard() }.map { (index, it) -> it.toScoreboardDiff(snapshot = index == 0) })
    }

    private fun ContestStateWithScoreboard.isAffectingScoreboard() : Boolean {
        return scoreboardRowsChanged.isNotEmpty() || rankingAfter !== rankingBefore
    }

    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        val mainScoreboardFlow = flow.shareIn(this, SharingStarted.Eagerly, replay = 1)
        setUp(OptimismLevel.NORMAL, mainScoreboardFlow)
        launch {
            val first = mainScoreboardFlow.mapNotNull { it.state.infoAfterEvent }.first()
            when (first.resultType) {
                ContestResultType.IOI -> {
                    setUp(OptimismLevel.OPTIMISTIC, mainScoreboardFlow)
                    setUp(OptimismLevel.PESSIMISTIC, mainScoreboardFlow)
                }
                ContestResultType.ICPC -> {
                    log.info { "It is ICPC contest, start also calculating secondary scoreboards" }
                    for (level in listOf(OptimismLevel.PESSIMISTIC, OptimismLevel.OPTIMISTIC)) {
                        val secondaryScoreboardFlow = mainScoreboardFlow.map { it.state.lastEvent }.calculateScoreboard(level).shareIn(this, SharingStarted.Eagerly, replay = 1)
                        setUp(level, secondaryScoreboardFlow)
                    }
                }
            }
        }
    }

    companion object {
        val log by getLogger()
    }
}


