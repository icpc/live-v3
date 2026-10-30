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

public data class Ranking(
    public val order: List<TeamId>,
    public val ranks: List<Int>,
    public val awards: List<Award>,
)

public interface ScoreboardCalculator {
    public fun getScoreboardRow(runs: List<RunInfo>): ScoreboardRow
    /*
     * Order hint can be used to improve performance. It doesn't affect and result,
     * but if it's close to actual sorting (e.g. sorting from before the last change),
     * call would be significantly faster.
     */
    public fun getRanking(rows: Map<TeamId, ScoreboardRow>, orderHint: List<TeamId> = emptyList()): Ranking
    public fun affectsRanking(old: ScoreboardRow, new: ScoreboardRow): Boolean
}

private fun <T> List<T>.intersectsWith(other: Set<T>): Boolean {
    return any { other.contains(it) }
}

internal abstract class AbstractScoreboardCalculator(protected val info: ContestInfo) : ScoreboardCalculator {

    abstract fun compare(a: ScoreboardRow, b: ScoreboardRow): Int

    protected fun ScoreboardRow.shouldBeHiddenAsNoSubmissions() : Boolean {
        return !info.showTeamsWithoutSubmissions && problemResults.all { it.lastSubmitTime == null }
    }

    private inner class RankingEntry(
        val team: TeamInfo,
        val row: ScoreboardRow
    ) : Comparable<RankingEntry> {
        var rank: Int = 0
        var index: Int = -1
        val organization = info.organizations[team.organizationId]

        override fun compareTo(other: RankingEntry): Int {
            val byRow = compare(row, other.row)
            if (byRow != 0) return byRow
            val byName = team.displayName.compareTo(other.team.displayName)
            if (byName != 0) return byName
            return team.id.value.compareTo(other.team.id.value)
        }
    }



    @OptIn(InefficientContestInfoApi::class)
    override fun getRanking(
        rows: Map<TeamId, ScoreboardRow>,
        orderHint: List<TeamId>,
    ): Ranking {
        val entries = buildList(info.teamList.size) {
            fun isShown(team: TeamInfo, row: ScoreboardRow?): Boolean {
                return !team.isHidden && row != null && !row.shouldBeHiddenAsNoSubmissions()
            }
            for (teamId in orderHint) {
                val team = info.teams[teamId] ?: continue
                val row = rows[teamId] ?: continue
                if (!isShown(team, row)) continue
                add(RankingEntry(team, row))
            }
            if (size != info.teamList.count { isShown(it, rows[it.id]) }) {
                val alreadyAdded = orderHint.toSet()
                for (team in info.teamList) {
                    if (team.id in alreadyAdded) continue
                    val row = rows[team.id] ?: continue
                    if (!isShown(team, row)) continue
                    add(RankingEntry(team, row))
                }
            }
            sort()
        }

        var nextRank = 1
        var right = 0
        while (right < entries.size) {
            val left = right
            while (right < entries.size && compare(entries[left].row, entries[right].row) == 0) {
                right++
            }
            val curRank = nextRank
            for (i in left until right) {
                entries[i].index = i
                if (!entries[i].team.isOutOfContest) {
                    entries[i].rank = curRank
                    nextRank++
                }
            }
        }

        val awards = buildList {
            for (chain in info.awardsSettings) {
                if (chain.awards.isEmpty()) continue
                val medalGroupSet = chain.groups.takeIf { it.isNotEmpty() }?.toSet()
                val medalExcludedGroupSet = chain.excludedGroups.takeIf { it.isNotEmpty() }?.toSet()
                val entriesFilteredByGroup = entries.filter {
                    val team = it.team
                    !team.isOutOfContest &&
                            (medalGroupSet == null || team.groups.intersectsWith(medalGroupSet)) &&
                            (medalExcludedGroupSet == null || !team.groups.intersectsWith(medalExcludedGroupSet))
                }
                val chainAwarded = BooleanArray(entries.size)
                var chainAwardedCount = 0
                val chainOrganizationAwarded = mutableMapOf<OrganizationId, Int>()
                for (award in chain.awards) {
                    val awarded = BooleanArray(entries.size)
                    val awardedTeams = mutableListOf<TeamId>()
                    var awardedCount = 0
                    val organizationAwarded = mutableMapOf<OrganizationId, Int>()
                    val chainLimit = award.chainLimit ?: Int.MAX_VALUE
                    val limit = award.limit ?: Int.MAX_VALUE
                    val organizationLimitField = award.organizationLimitCustomField
                    val chainOrganizationLimitField = award.chainOrganizationLimitCustomField
                    @IgnorableReturnValue
                    fun processChunk(chunk: List<RankingEntry>) : Boolean {
                        if (award.tiebreakMode == AwardTiebreakMode.NONE) {
                            if (chainAwardedCount + chunk.size > chainLimit) return false
                            if (awardedCount + chunk.size > limit) return false
                        }
                        for (entry in chunk) {
                            if (!awarded[entry.index]) {
                                awarded[entry.index] = true
                                awardedTeams.add(entry.team.id)
                                awardedCount++
                            }
                            if (!chainAwarded[entry.index]) {
                                chainAwarded[entry.index] = true
                                chainAwardedCount++
                            }
                            val organizationId = entry.team.organizationId
                            if (organizationId != null) {
                                organizationAwarded[organizationId] = (organizationAwarded[organizationId] ?: 0) + 1
                                chainOrganizationAwarded[organizationId] = (chainOrganizationAwarded[organizationId] ?: 0) + 1
                            }
                        }
                        if (award.tiebreakMode == AwardTiebreakMode.ALL) {
                            if (chainAwardedCount >= chainLimit) return false
                            if (awardedCount >= limit) return false
                        }
                        return true
                    }
                    if (award.manualTeamIds.isNotEmpty()) {
                        processChunk(entries.filter { award.manualTeamIds.contains(it.team.id) && !chainAwarded[it.index] })
                    }
                    var currentChunkRank = -1
                    val currentChunk = mutableListOf<RankingEntry>()
                    for (entry in entriesFilteredByGroup) {
                        if (chainAwarded[entry.index]) continue
                        if (award.maxRank != null && entry.rank > award.maxRank) break
                        if (award.minScore != null && entry.row.totalScore < award.minScore) break
                        if (currentChunkRank != entry.rank) {
                            if (!processChunk(currentChunk)) {
                                currentChunk.clear()
                                break
                            }
                            currentChunkRank = entry.rank
                            currentChunk.clear()
                        }
                        entry.organization?.let { org ->
                            val chainOrganizationLimit = chainOrganizationLimitField
                                ?.let { field -> org.customFields[field]?.toIntOrNull() }
                                ?: award.chainOrganizationLimit
                            if (chainOrganizationLimit != null && (chainOrganizationAwarded[org.id] ?: 0) >= chainOrganizationLimit) {
                                continue
                            }
                            val organizationLimit = organizationLimitField
                                ?.let { field -> org.customFields[field]?.toIntOrNull() }
                                ?: award.organizationLimit
                            if (organizationLimit != null && (organizationAwarded[org.id] ?: 0) >= organizationLimit) {
                                continue
                            }
                        }
                        currentChunk.add(entry)
                    }
                    processChunk(currentChunk)

                    add(Award(award.id, award.citation, awardedTeams.toSet()))
                }
            }
        }

        return Ranking(entries.map { it.team.id }, entries.map { it.rank }, awards)
    }

    companion object {
        val logger by getLogger()
    }
}

public fun getScoreboardCalculator(info: ContestInfo, optimismLevel: OptimismLevel): ScoreboardCalculator =
    when (info.resultType) {
        ContestResultType.ICPC -> when (optimismLevel) {
            OptimismLevel.NORMAL -> ICPCNormalScoreboardCalculator(info)
            OptimismLevel.OPTIMISTIC -> ICPCOptimisticScoreboardCalculator(info)
            OptimismLevel.PESSIMISTIC -> ICPCPessimisticScoreboardCalculator(info)
        }

        ContestResultType.IOI -> IOIScoreboardCalculator(info)
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
        var rankingAffected = state.lastEvent is InfoUpdate
        val teamsReallyAffected = teamsAffected.filter {
            val newRow = calculator.getScoreboardRow(runsByTeamId.getRuns(it))
            val oldRow = rows[it]
            val infoBefore = state.infoBeforeEvent?.teams[it]
            val infoAfter = state.infoAfterEvent.teams[it]
            if (oldRow != newRow) {
                rows = rows.putting(it, newRow)
            }
            val teamInfoChanged = infoBefore?.isOutOfContest != infoAfter?.isOutOfContest ||
                    infoBefore?.customFields != infoAfter?.customFields ||
                    infoBefore?.isHidden != infoAfter?.isHidden
            if (oldRow == null || calculator.affectsRanking(oldRow, newRow)) {
                rankingAffected = true
            }
            oldRow != newRow || teamInfoChanged
        }
        if (rankingAffected) {
            val newRanking = calculator.getRanking(rows, lastRanking.order)
            // upstream clients would be able to rely on faster identity checks for optimizations
            if (newRanking != lastRanking) {
                lastRanking = newRanking
            }
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


