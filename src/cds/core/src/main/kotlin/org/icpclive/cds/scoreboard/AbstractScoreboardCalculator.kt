package org.icpclive.cds.scoreboard

import kotlinx.collections.immutable.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.cds.*
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.util.getLogger
import kotlin.time.Duration

public class Ranking internal constructor(
    public val order: List<TeamId>,
    public val ranks: List<Int>,
    public val awards: List<Award>,
)

public interface ScoreboardCalculator {
    public fun getScoreboardRow(info: ContestInfo, runs: List<RunInfo>): ScoreboardRow
    public fun getRanking(info: ContestInfo, rows: Map<TeamId, ScoreboardRow>): Ranking
}

private fun ordinalText(x: Int) = if (x in 11..13) {
    "$x-th"
} else {
    val r = x % 10
    val suffix = when (r) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    "$x-$suffix"
}

internal abstract class AbstractScoreboardCalculator : ScoreboardCalculator {

    abstract val comparator: Comparator<ScoreboardRow>

    @OptIn(InefficientContestInfoApi::class)
    override fun getRanking(info: ContestInfo, rows: Map<TeamId, ScoreboardRow>): Ranking {
        val comparatorWithName = compareBy(comparator) { it: Pair<TeamId, ScoreboardRow> -> it.second }
            .thenBy { info.teams[it.first]!!.displayName }
        val orderList = info.teamList
            .filterNot { it.isHidden }
            .map { it.id to rows[it.id]!! }
            .sortedWith(comparatorWithName)

        val order = orderList.map { it.first }
        val ranks = MutableList(order.size) { 0 }
        val orderedRows = orderList.map { it.second }

        val firstGroupRank = mutableMapOf<GroupId, Int>()

        var nextRank = 1
        var right = 0
        while (right < order.size) {
            val left = right
            while (right < order.size && comparator.compare(orderedRows[left], orderedRows[right]) == 0) {
                right++
            }
            val curRank = nextRank
            for (i in left until right) {
                val team = info.teams[order[i]]!!
                if (!team.isOutOfContest) {
                    ranks[i] = curRank
                    nextRank++
                    for (group in team.groups) {
                        firstGroupRank.putIfAbsent(group, curRank)
                    }
                }
            }
        }

        val awardsSettings = info.awardsSettings
        val teamRanks = (0 until ranks.size).groupBy({ ranks[it] }, { order[it] })
        val awards = buildList {
            awardsSettings.championTitle?.let { title ->
                add(Award.Winner("winner", title, teamRanks[1]?.toSet() ?: emptySet()))
            }
            for ((groupId, title) in awardsSettings.groupsChampionTitles) {
                val groupBestRank = firstGroupRank[groupId]
                add(
                    Award.GroupChampion(
                        id = "group-winner-$groupId",
                        citation = title,
                        groupId = groupId,
                        teams = teamRanks[groupBestRank]!!.filter { groupId in info.teams[it]!!.groups }.toSet()
                    )
                )
            }
            for (medalGroup in awardsSettings.medalSettings) {
                var position = 1
                for (medal in medalGroup) {
                    val rankLimit = medal.maxRank ?: nextRank
                    val teams = buildSet {
                        while (position <= rankLimit) {
                            val teams = teamRanks[position] ?: break
                            if (rows[teams[0]]!!.totalScore < medal.minScore) break
                            if (medal.tiebreakMode == AwardsSettings.MedalTiebreakMode.NONE && (position + teams.size - 1) > rankLimit) break
                            position += teams.size
                            addAll(teams)
                        }
                    }
                    add(
                        Award.Medal(
                            medal.id,
                            medal.citation,
                            medal.color,
                            teams
                        )
                    )
                }
            }
            for (rank in 1..awardsSettings.rankAwardsMaxRank) {
                add(
                    Award.Custom(
                        "rank-$rank",
                        "${ordinalText(rank)} place",
                        teamRanks[rank]?.toSet() ?: emptySet()
                    )
                )
            }
            for (manual in awardsSettings.manual) {
                add(
                    Award.Custom(
                        manual.id,
                        manual.citation,
                        manual.teamCdsIds.toSet()
                    )
                )
            }
        }

        return Ranking(order, ranks.toList(), awards)
    }

    companion object {
        val logger = getLogger(AbstractScoreboardCalculator::class)
    }
}

public fun getScoreboardCalculator(info: ContestInfo, optimismLevel: OptimismLevel): ScoreboardCalculator =
    when (info.resultType) {
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
            medalType = awards.firstNotNullOfOrNull { e -> (e as? Award.Medal)?.medalColor?.takeIf { teamId in e.teams }?.name?.lowercase() },
            championInGroups = awards.mapNotNull { e -> (e as? Award.GroupChampion)?.groupId?.takeIf { teamId in e.teams } },
            problemResults = row.problemResults,
            teamGroups = info.teams[teamId]!!.groups
        )
    }
)

private class RedoTask(
    val info: ContestInfo,
    val mode: ScoreboardUpdateType,
    val runs: PersistentMap<TeamId, PersistentList<RunInfo>>,
    val lastSubmissionTime: Duration,
)


private fun Flow<ContestUpdate>.teamRunsUpdates() = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<TeamId, PersistentList<RunInfo>>()
    var lastSubmissionTime = Duration.ZERO
    val oldKey = mutableMapOf<RunId, TeamId>()
    collect { update ->
        suspend fun updateGroup(key: TeamId) {
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
                emit(
                    RedoTask(
                        update.newInfo,
                        ScoreboardUpdateType.SNAPSHOT,
                        curRuns,
                        lastSubmissionTime
                    )
                )
            }

            is AnalyticsUpdate -> {}
        }
    }
}

public class ScoreboardAndContestInfo internal constructor(
    public val info: ContestInfo,
    public val scoreboardSnapshot: Scoreboard,
    public val scoreboardDiff: Scoreboard,
    public val lastSubmissionTime: Duration,
)

public fun Flow<ContestUpdate>.calculateScoreboard(optimismLevel: OptimismLevel): Flow<ScoreboardAndContestInfo> =
    flow {
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

            var rows = persistentMapOf<TeamId, ScoreboardRow>()
            val logger = getLogger(AbstractScoreboardCalculator::class)
            while (true) {
                val task = s.getAndUpdate { null } ?: s.filterNotNull().first().let { s.getAndUpdate { null }!! }
                logger.info("Recalculating scoreboard mode = ${task.mode}, patch_rows = ${task.runs.size}, teams = ${task.info.teams.size}, lastSubmissionTime = ${task.lastSubmissionTime}")
                val calculator = getScoreboardCalculator(task.info, optimismLevel)
                val teams = when (task.mode) {
                    ScoreboardUpdateType.DIFF -> task.runs
                    ScoreboardUpdateType.SNAPSHOT -> task.info.teams
                }.keys.filterNot { task.info.teams[it]!!.isHidden }
                val upd = teams.associateWithTo(persistentMapOf<TeamId, ScoreboardRow>().builder()) {
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
                for (team in teams) {
                    require(team in rows) { "team $team is not in rows" }
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


