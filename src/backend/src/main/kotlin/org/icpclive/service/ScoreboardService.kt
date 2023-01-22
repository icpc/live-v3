package org.icpclive.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.util.getLogger

private fun List<MedalType>.medalColorByRank(rank_: Int): String? {
    var rank = rank_
    for ((color, count) in this) {
        if (rank <= count) return color
        rank -= count
    }
    return null
}

abstract class ScoreboardService(val optimismLevel: OptimismLevel) {
    val flow = MutableStateFlow(Scoreboard(emptyList())).also {
        DataBus.setScoreboardEvents(optimismLevel, it)
    }
    val runs = mutableMapOf<Int, RunInfo>()

    suspend fun run(
        runsFlow: Flow<RunInfo>,
        contestInfoFlow: Flow<ContestInfo>,
    ) {
        logger.info("Scoreboard service for optimismLevel=${optimismLevel} started")
        var info: ContestInfo? = null
        merge(runsFlow, contestInfoFlow).collect { update ->
            when (update) {
                is RunInfo -> {
                    val oldRun = runs[update.id]
                    runs[update.id] = update
                    if (oldRun != null && oldRun.result == update.result) {
                        return@collect
                    }
                }
                is ContestInfo -> {
                    info = update
                }
            }
            info?.let {
                flow.value = getScoreboard(it)
            }
        }
    }

    abstract fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow

    abstract val comparator : Comparator<ScoreboardRow>

    private fun getScoreboard(info: ContestInfo): Scoreboard {
        val runs = runs.values
            .sortedWith(compareBy({ it.time }, { it.id }))
            .groupBy { it.teamId }
        val teamsInfo = info.teams.filterNot { it.isHidden }.associateBy { it.id }

        val allGroups = teamsInfo.values.flatMap { it.groups }.toMutableSet()

        val rows = teamsInfo.values
            .map { info.getScoreboardRow(it.id, runs[it.id] ?: emptyList(), it.groups, info.problems) }
            .sortedWith(comparator.thenComparing { it: ScoreboardRow -> teamsInfo[it.teamId]!!.name })
            .toMutableList()
        if (rows.isNotEmpty()) {
            var rank = 1
            for (i in 0 until rows.size) {
                if (i != 0 && comparator.compare(rows[i - 1], rows[i]) < 0) {
                    rank = i + 1
                }
                val medal = info.medals.medalColorByRank(rank)?.takeIf { rows[i].totalScore > 0 }
                val championInGroups = if (rows[i].totalScore > 0)
                    teamsInfo[rows[i].teamId]!!.groups.filter { it in allGroups }
                else
                    emptyList()
                for (group in championInGroups) {
                    allGroups -= group
                }
                rows[i] = rows[i].copy(rank = rank, medalType = medal, championInGroups = championInGroups)
            }
        }
        return Scoreboard(rows)
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}
