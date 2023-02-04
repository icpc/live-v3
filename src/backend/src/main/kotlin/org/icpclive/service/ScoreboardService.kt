package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
                .map { getScoreboard(it, mutex.withLock { sortSubmissions(runs.values) }) }
                .stateIn(this)
                .let { DataBus.setScoreboardEvents(optimismLevel, it) }
        }
    }

    private fun sortSubmissions(runs: Iterable<RunInfo>) = runs
        .sortedWith(compareBy(RunInfo::time, RunInfo::id))
        .groupBy(RunInfo::teamId)


    abstract fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow

    abstract val comparator : Comparator<ScoreboardRow>

    private fun getScoreboard(info: ContestInfo, runs: Map<Int, List<RunInfo>>): Scoreboard {
        logger.info("Calculating scoreboard: runs count = ${runs.values.sumOf { it.size }}")
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
