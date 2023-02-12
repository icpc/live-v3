package org.icpclive.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.util.getLogger


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
        fun ScoreboardRow.team() = teamsInfo[teamId]!!

        val hasChampion = mutableSetOf<String>()

        val rows = teamsInfo.values
            .map { info.getScoreboardRow(it.id, runs[it.id] ?: emptyList(), it.groups, info.problems) }
            .sortedWith(comparator.thenComparing { it -> it.team().name })
            .toMutableList()
        var left: Int
        var right = 0
        while (right < rows.size) {
            left = right
            while (right < rows.size && comparator.compare(rows[left], rows[right]) == 0) {
                right++
            }
            val medal = run {
                var skipped = 0
                for (type in info.medals) {
                    val canGetMedal = when (type.tiebreakMode) {
                        MedalTiebreakMode.ALL -> left < type.count + skipped
                        MedalTiebreakMode.NONE -> right <= type.count + skipped
                    } && rows[left].totalScore >= type.minScore
                    if (canGetMedal) {
                        return@run type.name
                    }
                    skipped += type.count
                }
                null
            }
            for (i in left until right) {
                rows[i] = rows[i].copy(
                    rank = left + 1,
                    medalType = medal,
                    championInGroups = teamsInfo[rows[i].teamId]!!.groups.filter { it !in hasChampion }
                )
            }
            for (i in left until right) {
                hasChampion.addAll(teamsInfo[rows[i].teamId]!!.groups)
            }
        }
        return Scoreboard(rows)
    }

    companion object {
        val logger = getLogger(ScoreboardService::class)
    }
}
