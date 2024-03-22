package org.icpclive.cds.scoreboard

import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.*
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

public fun ContestStateWithScoreboard.toScoreboardDiff(snapshot: Boolean) : ScoreboardDiff {
    val rows = if (snapshot) {
        scoreboardRows
    } else {
        scoreboardRowsDiff.associateWith { scoreboardRows[it]!! }
    }
    return ScoreboardDiff(rows, order, ranks, awards)
}

public fun ContestStateWithScoreboard.toLegacyScoreboard(): LegacyScoreboard = LegacyScoreboard(
    order.zip(ranks).map { (teamId, rank) ->
        val row = scoreboardRows[teamId]!!
        LegacyScoreboardRow(
            teamId = teamId,
            rank = rank,
            totalScore = row.totalScore,
            penalty = row.penalty,
            lastAccepted = row.lastAccepted.inWholeMilliseconds,
            medalType = awards.firstNotNullOfOrNull { e -> (e as? Award.Medal)?.medalColor?.takeIf { teamId in e.teams }?.name?.lowercase() },
            championInGroups = awards.mapNotNull { e -> (e as? Award.GroupChampion)?.groupId?.takeIf { teamId in e.teams } },
            problemResults = row.problemResults,
            teamGroups = state.infoAfterEvent!!.teams[teamId]!!.groups
        )
    }
)

public class ContestStateWithScoreboard internal constructor(
    public val state: ContestState,
    public val scoreboardRows: Map<TeamId, ScoreboardRow>,
    public val scoreboardRowsDiff: List<TeamId>,
    public val order: List<TeamId>,
    public val ranks: List<Int>,
    public val awards: List<Award>,
    public val lastSubmissionTime: Duration,
)

public fun Flow<ContestUpdate>.calculateScoreboard(optimismLevel: OptimismLevel): Flow<ContestStateWithScoreboard> = flow {
    var rows = persistentMapOf<TeamId, ScoreboardRow>()
    var lastRanking = Ranking(emptyList(), emptyList(), emptyList())
    var lastSubmissionTime: Duration = Duration.ZERO
    val logger = getLogger(AbstractScoreboardCalculator::class)
    var runsByTeamId = persistentMapOf<TeamId, PersistentList<RunInfo>>()
    fun applyEvent(state: ContestState) : List<TeamId> {
        val info = state.infoAfterEvent ?: return emptyList()
        val calculator = getScoreboardCalculator(info, optimismLevel)
        val teamsAffected = when (val event = state.lastEvent) {
            is AnalyticsUpdate -> emptyList()
            is InfoUpdate -> info.teams.keys.toList()
            is RunUpdate -> {
                val oldRun = state.runsBeforeEvent[event.newInfo.id]
                val newRun = event.newInfo
                if (oldRun?.teamId != newRun.teamId) {
                    if (oldRun != null) {
                        runsByTeamId = runsByTeamId.removeRun(oldRun.teamId, oldRun)
                    }
                    runsByTeamId = runsByTeamId.addAndResort(newRun.teamId, newRun)
                } else {
                    runsByTeamId = runsByTeamId.updateAndResort(newRun.teamId, newRun)
                }
                lastSubmissionTime = maxOf(lastSubmissionTime, newRun.time)

                val oldRunTested = oldRun != null && !oldRun.isHidden && oldRun.result !is RunResult.InProgress
                val newRunTested = !newRun.isHidden && newRun.result !is RunResult.InProgress
                if (!oldRunTested && !newRunTested)
                    emptyList()
                else {
                    listOfNotNull(oldRun?.teamId, newRun.teamId).distinct()
                }
            }
        }
        if (teamsAffected.isNotEmpty()) {
            logger.info("Recalculating $optimismLevel scoreboard for ${teamsAffected.size} teams, lastSubmissionTime = $lastSubmissionTime")
        }
        val teamsReallyAffected = teamsAffected.filter {
            val newRow = calculator.getScoreboardRow(info, runsByTeamId[it] ?: emptyList())
            val oldRow = rows[it]
            rows = rows.put(it, newRow)
            newRow != oldRow
        }
        if (teamsReallyAffected.isNotEmpty()) {
            lastRanking = calculator.getRanking(info, rows)
        }
        return teamsReallyAffected
    }
    contestState().collect {
        val scoreboardDiff = applyEvent(it)
        emit(
            ContestStateWithScoreboard(
                state = it,
                scoreboardRows = rows,
                scoreboardRowsDiff = scoreboardDiff,
                order = lastRanking.order,
                ranks = lastRanking.ranks,
                awards = lastRanking.awards,
                lastSubmissionTime = lastSubmissionTime
            )
        )
    }
}


