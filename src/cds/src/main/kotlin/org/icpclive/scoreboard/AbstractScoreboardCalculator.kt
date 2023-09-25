package org.icpclive.scoreboard

import kotlinx.collections.immutable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.cds.adapters.*
import org.icpclive.util.getLogger
import kotlin.time.Duration

public data class Ranking(
    val order: List<Int>,
    val ranks: List<Int>,
    val awards: Map<Award, Set<Int>>
)

public interface ScoreboardCalculator {
    public fun getScoreboardRow(info: ContestInfo, runs: List<RunInfo>): ScoreboardRow
    public fun getRanking(info: ContestInfo, rows: Map<Int, ScoreboardRow>): Ranking
}

internal abstract class AbstractScoreboardCalculator : ScoreboardCalculator {

    abstract val comparator : Comparator<ScoreboardRow>

    @OptIn(InefficientContestInfoApi::class)
    override fun getRanking(info: ContestInfo, rows: Map<Int, ScoreboardRow>): Ranking {
        val hasChampion = mutableSetOf<String>()
        val order_ = info.teamList.filterNot { it.isHidden }.map { it.id to rows[it.id]!! }.sortedWith { a, b ->
            comparator.compare(a.second, b.second)
        }
        val awards = buildMap<Award, MutableSet<Int>> {
            put(Award.Winner, mutableSetOf())
            info.medals.forEach { put(Award.Medal(it.name), mutableSetOf()) }
            info.groupList.forEach { if (it.awardsGroupChampion) put(Award.GroupChampion(it.cdsId), mutableSetOf()) }
        }
        val order = order_.map { it.first }
        val ranks = MutableList(order.size) { 0 }
        val orderedRows = order_.map { it.second }

        var right = 0
        var outOfContestTeams = 0
        while (right < order.size) {
            val left = right
            while (right < order.size && comparator.compare(orderedRows[left], orderedRows[right]) == 0) {
                right++
            }
            val minRank = left + 1 - outOfContestTeams
            val nextRank = minRank + order.subList(left, right).count { !info.teams[it]!!.isOutOfContest }
            var skipped = 0
            val totalScore = orderedRows[left].totalScore
            for (type in info.medals) {
                val canGetMedal = when (type.tiebreakMode) {
                    MedalTiebreakMode.ALL -> minRank <= type.count + skipped
                    MedalTiebreakMode.NONE -> nextRank <= type.count + skipped + 1
                } && totalScore >= type.minScore
                if (canGetMedal) {
                    awards[Award.Medal(type.name)]!!.addAll(order.subList(left, right).filterNot { info.teams[it]!!.isOutOfContest })
                    break
                }
                skipped += type.count
            }
            for (i in left until right) {
                val team = info.teams[order[i]]!!
                if (team.isOutOfContest) {
                    outOfContestTeams++
                } else {
                    if (minRank == 1 && totalScore > 0) {
                        awards[Award.Winner]?.add(order[i])
                    }
                    ranks[i] = minRank
                    for (it in team.groups) {
                        if (it !in hasChampion) {
                            awards[Award.GroupChampion(it)]?.add(order[i])
                        }
                    }
                }
            }
            for (i in left until right) {
                hasChampion.addAll(info.teams[order[i]]!!.groups)
            }
        }
        return Ranking(order, ranks.toList(), awards.mapValues { (_, v) -> v.toSet() })
    }

    companion object {
        val logger = getLogger(AbstractScoreboardCalculator::class)
    }
}

public fun getScoreboardCalculator(info: ContestInfo, optimismLevel: OptimismLevel) : ScoreboardCalculator = when (info.resultType) {
    ContestResultType.ICPC -> when (optimismLevel) {
        OptimismLevel.NORMAL -> ICPCNormalScoreboardCalculator()
        OptimismLevel.OPTIMISTIC -> ICPCOptimisticScoreboardCalculator()
        OptimismLevel.PESSIMISTIC -> ICPCPessimisticScoreboardCalculator()
    }
    ContestResultType.IOI -> IOIScoreboardCalculator()
}

public fun Scoreboard.toLegacyScoreboard(info: ContestInfo): LegacyScoreboard = LegacyScoreboard(
    order.zip(ranks).map { (teamId, rank) ->
        val row = rows[teamId]!!
        LegacyScoreboardRow(
            teamId = teamId,
            rank = rank,
            totalScore = row.totalScore,
            penalty = row.penalty,
            lastAccepted = row.lastAccepted.inWholeMilliseconds,
            medalType = awards.entries.firstNotNullOfOrNull { e -> (e.key as? Award.Medal)?.medalType?.takeIf { teamId in e.value } },
            championInGroups = awards.entries.mapNotNull { e -> (e.key as? Award.GroupChampion)?.group?.takeIf { teamId in e.value } },
            problemResults = row.problemResults,
            teamGroups = info.teams[teamId]!!.groups
        )
    }
)

private class RedoTask(
    val info: ContestInfo,
    val mode: ScoreboardUpdateType,
    val runs: PersistentMap<Int, PersistentList<RunInfo>>,
    val lastSubmissionTime: Duration
)


private fun Flow<ContestUpdate>.teamRunsUpdates() = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<Int, PersistentList<RunInfo>>()
    var lastSubmissionTime = Duration.ZERO
    val oldKey = mutableMapOf<Int, Int>()
    collect { update ->
        suspend fun updateGroup(key: Int) {
            val info = curInfo ?: return
            emit(
                RedoTask(
                    info,
                    ScoreboardUpdateType.DIFF,
                    persistentMapOf(key to (curRuns[key] ?: persistentListOf())),
                    lastSubmissionTime,
                )
            )
        }
        when (update) {
            is RunUpdate -> {
                lastSubmissionTime = maxOf(lastSubmissionTime, update.newInfo.time)
                val k = update.newInfo.teamId
                val oldK = oldKey[update.newInfo.id]
                oldKey[update.newInfo.id] = k
                if (oldK != k) {
                    if (oldK != null) {
                        curRuns = curRuns.removeRun(oldK, update.newInfo)
                        updateGroup(oldK)
                    }
                    curRuns = curRuns.addAndResort(k, update.newInfo)
                    updateGroup(k)
                } else {
                    curRuns = curRuns.updateAndResort(k, update.newInfo)
                    updateGroup(k)
                }
            }
            is InfoUpdate -> {
                curInfo = update.newInfo
                emit(RedoTask(
                    update.newInfo,
                    ScoreboardUpdateType.SNAPSHOT,
                    curRuns,
                    lastSubmissionTime
                ))
            }
            is AnalyticsUpdate -> {}
        }
    }
}

public data class ScoreboardAndContestInfo(
    val info: ContestInfo,
    val scoreboardSnapshot: Scoreboard,
    val scoreboardDiff: Scoreboard,
    val lastSubmissionTime: Duration
)

public fun Flow<ContestUpdate>.calculateScoreboard(optimismLevel: OptimismLevel): Flow<ScoreboardAndContestInfo> = flow {
    coroutineScope {
        val s = MutableStateFlow<RedoTask?>(null)
        launch {
            teamRunsUpdates()
                .collect {
                    s.update { old ->
                        if (it.mode == ScoreboardUpdateType.SNAPSHOT || old == null) {
                            it
                        } else {
                            RedoTask(it.info, old.mode, old.runs.putAll(it.runs), it.lastSubmissionTime)
                        }
                    }
                }
        }

        var rows = persistentMapOf<Int, ScoreboardRow>()
        val logger = getLogger(AbstractScoreboardCalculator::class)
        while (true) {
            val task = s.getAndUpdate { null } ?: s.filterNotNull().first().let { s.getAndUpdate { null }!! }
            logger.info("Recalculating scoreboard mode = ${task.mode}, patch_rows = ${task.runs.size}, teams = ${task.info.teams.size}, lastSubmissionTime = ${task.lastSubmissionTime}")
            val calculator = getScoreboardCalculator(task.info, optimismLevel)
            val teams = when (task.mode) {
                ScoreboardUpdateType.DIFF -> task.runs
                ScoreboardUpdateType.SNAPSHOT -> task.info.teams
            }.keys.toList()
            val upd = teams.associateWithTo(persistentMapOf<Int, ScoreboardRow>().builder()) {
                calculator.getScoreboardRow(
                    task.info,
                    task.runs[it] ?: emptyList()
                )
            }.build()
            rows = if (task.mode == ScoreboardUpdateType.SNAPSHOT) {
                upd
            } else {
                rows.putAll(upd)
            }
            task.info.teams.keys.firstOrNull { it !in rows }?.let {
                require(false) { "team $it is not in rows" }
            }
            val ranking = getScoreboardCalculator(task.info, optimismLevel).getRanking(task.info, rows)
            emit(
                ScoreboardAndContestInfo(
                    task.info,
                    Scoreboard(ScoreboardUpdateType.SNAPSHOT, rows, ranking.order, ranking.ranks, ranking.awards),
                    Scoreboard(task.mode, upd, ranking.order, ranking.ranks, ranking.awards),
                    task.lastSubmissionTime,
                )
            )
        }
    }
}


