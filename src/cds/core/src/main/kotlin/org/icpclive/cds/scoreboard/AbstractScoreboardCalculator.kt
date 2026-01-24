package org.icpclive.cds.scoreboard

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.icpclive.cds.*
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.utils.TeamRunsStorage
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

private fun <T> List<T>.intersectsWith(other: Set<T>): Boolean {
    return any { other.contains(it) }
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
            .run { if (info.showTeamsWithoutSubmissions) this else filter { it.second.problemResults.any { it.lastSubmitTime != null } } }
            .sortedWith(comparatorWithName)

        val order = orderList.map { it.first }
        val ranks = MutableList(order.size) { 0 }
        val orderedRows = orderList.map { it.second }

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
                }
            }
        }

        val awards = buildList {
            for (chain in info.awardsSettings) {
                if (chain.awards.isEmpty()) continue
                val medalGroupSet = chain.groups.toSet()
                val medalExcludedGroupSet = chain.excludedGroups.toSet()
                val indicesFilteredByGroup = buildList {
                    for ((index, teamId) in order.withIndex()) {
                        val team = info.teams[teamId]!!
                        if (team.isOutOfContest) continue
                        if (chain.groups.isNotEmpty() && !team.groups.intersectsWith(medalGroupSet)) continue
                        if (team.groups.intersectsWith(medalExcludedGroupSet)) continue
                        add(index)
                    }
                }
                val chainAwarded = mutableSetOf<Int>()
                val chainOrganizationAwarded = mutableMapOf< OrganizationId, Int>()
                for (award in chain.awards) {
                    val awarded = mutableSetOf<Int>()
                    val organizationAwarded = mutableMapOf<OrganizationId, Int>()
                    @IgnorableReturnValue
                    fun processChunk(chunk: List<Int>) : Boolean {
                        if (award.tiebreakMode == AwardTiebreakMode.NONE) {
                            if (chainAwarded.size + chunk.size > (award.chainLimit ?: Int.MAX_VALUE)) return false
                            if (awarded.size + chunk.size > (award.limit ?: Int.MAX_VALUE)) return false
                        }
                        for (i in chunk) {
                            awarded.add(i)
                            chainAwarded.add(i)
                            val team = info.teams[order[i]]!!
                            if (team.organizationId != null) {
                                organizationAwarded[team.organizationId] = (organizationAwarded[team.organizationId] ?: 0) + 1
                                chainOrganizationAwarded[team.organizationId] = (chainOrganizationAwarded[team.organizationId] ?: 0) + 1
                            }
                        }
                        if (award.tiebreakMode == AwardTiebreakMode.ALL) {
                            if (chainAwarded.size >= (award.chainLimit ?: Int.MAX_VALUE)) return false
                            if (awarded.size >= (award.limit ?: Int.MAX_VALUE)) return false
                        }
                        return true
                    }
                    if (award.manualTeamIds.isNotEmpty()) {
                        processChunk(order.indices.filter { award.manualTeamIds.contains(order[it]) && !chainAwarded.contains(it) })
                    }
                    var currentChunkRank = -1
                    val currentChunk = mutableListOf<Int>()
                    for (it in indicesFilteredByGroup) {
                        if (chainAwarded.contains(it)) continue
                        if (award.maxRank != null && ranks[it] > award.maxRank) break
                        val teamId = order[it]
                        val row = rows[teamId]!!
                        val teamInfo = info.teams[teamId]!!
                        if (award.minScore != null && row.totalScore < award.minScore) break
                        if (currentChunkRank != ranks[it]) {
                            if (!processChunk(currentChunk)) {
                                currentChunk.clear()
                                break
                            }
                            currentChunkRank = ranks[it]
                            currentChunk.clear()
                        }
                        val org = info.organizations[teamInfo.organizationId]
                        if (org != null) {
                            val chainOrganizationLimit = org.customFields[award.chainOrganizationLimitCustomField]?.toIntOrNull() ?: award.chainOrganizationLimit
                            if (chainOrganizationLimit != null && (chainOrganizationAwarded[org.id] ?: 0) >= chainOrganizationLimit) {
                                continue
                            }
                            val organizationLimit = org.customFields[award.organizationLimitCustomField]?.toIntOrNull() ?: award.organizationLimit
                            if (organizationLimit != null && (organizationAwarded[org.id] ?: 0) >= organizationLimit) {
                                continue
                            }
                        }
                        currentChunk.add(it)
                    }
                    processChunk(currentChunk)

                    add(Award(award.id, award.citation, awarded.map { order[it] }.toSet()))
                }
            }
        }

        return Ranking(order, ranks.toList(), awards)
    }

    companion object {
        val logger by getLogger()
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
        rankingAfter.order.associateWith { scoreboardRowAfter(it) }
    } else {
        scoreboardRowsChanged.associateWith { scoreboardRowAfter(it) }
    }
    return ScoreboardDiff(rows, rankingAfter.order, rankingAfter.ranks, rankingAfter.awards)
}

/**
 * The class representing the state of the contest at some point.
 *
 * The state is stored both before and after the last event.
 *
 * Scoreboard rows map, as opposed to ranking, contains hidden teams, so it shouldn't be used directly.
 * Iterating over [Ranking.order] should be used instead.
 */
public class ContestStateWithScoreboard internal constructor(
    public val state: ContestState,
    internal val scoreboardRowsAfter: Map<TeamId, ScoreboardRow>,
    internal val scoreboardRowsBefore: Map<TeamId, ScoreboardRow>,
    public val scoreboardRowsChanged: List<TeamId>,
    public val rankingBefore: Ranking,
    public val rankingAfter: Ranking,
    public val lastSubmissionTime: Duration,
) {
    public fun scoreboardRowBeforeOrNull(teamId: TeamId): ScoreboardRow? = scoreboardRowsBefore[teamId]
    public fun scoreboardRowAfterOrNull(teamId: TeamId): ScoreboardRow? = scoreboardRowsAfter[teamId]
    public fun scoreboardRowBefore(teamId: TeamId): ScoreboardRow = scoreboardRowsBefore[teamId]!!
    public fun scoreboardRowAfter(teamId: TeamId): ScoreboardRow = scoreboardRowsAfter[teamId]!!
}

public fun Flow<ContestUpdate>.calculateScoreboard(optimismLevel: OptimismLevel): Flow<ContestStateWithScoreboard> = flow {
    var rows = persistentMapOf<TeamId, ScoreboardRow>()
    var lastRanking = Ranking(emptyList(), emptyList(), emptyList())
    var lastSubmissionTime: Duration = Duration.ZERO
    val runsByTeamId = TeamRunsStorage()
    fun applyEvent(state: ContestState) : List<TeamId> {
        val info = state.infoAfterEvent ?: return emptyList()
        val calculator = getScoreboardCalculator(info, optimismLevel)
        val teamsAffected = when (val event = state.lastEvent) {
            is CommentaryMessagesUpdate -> emptyList()
            is InfoUpdate -> info.teams.keys.toList()
            is RunUpdate -> {
                lastSubmissionTime = maxOf(lastSubmissionTime, event.newInfo.time)
                runsByTeamId.applyEvent(state)
            }
        }
        val teamsReallyAffected = teamsAffected.filter {
            val newRow = calculator.getScoreboardRow(info, runsByTeamId.getRuns(it))
            val oldRow = rows[it]
            val newInfo = state.infoBeforeEvent?.teams[it]
            val oldInfo = state.infoAfterEvent.teams[it]
            rows = rows.put(it, newRow)
            newRow != oldRow || newInfo?.isOutOfContest != oldInfo?.isOutOfContest || newInfo?.customFields != oldInfo?.customFields || newInfo?.isHidden != oldInfo?.isHidden
        }
        if (teamsReallyAffected.isNotEmpty() || state.infoBeforeEvent?.awardsSettings != state.infoAfterEvent.awardsSettings) {
            lastRanking = calculator.getRanking(info, rows)
        }
        return teamsReallyAffected
    }
    contestState().collect {
        val oldRows = rows
        val oldRanking = lastRanking
        val scoreboardDiff = applyEvent(it)
        emit(
            ContestStateWithScoreboard(
                state = it,
                scoreboardRowsAfter = rows,
                scoreboardRowsBefore = oldRows,
                scoreboardRowsChanged = scoreboardDiff,
                rankingAfter = lastRanking,
                rankingBefore = oldRanking,
                lastSubmissionTime = lastSubmissionTime
            )
        )
    }
}


