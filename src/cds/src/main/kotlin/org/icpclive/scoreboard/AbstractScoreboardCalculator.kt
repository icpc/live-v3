package org.icpclive.scoreboard

import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.adapters.ContestStateWithRunsByTeam
import org.icpclive.util.getLogger
import kotlin.time.Duration

interface ScoreboardCalculator {
    fun getScoreboard(info: ContestInfo, runs: Map<Int, List<RunInfo>>) : Scoreboard
}

internal abstract class AbstractScoreboardCalculator : ScoreboardCalculator {

    abstract fun ContestInfo.getScoreboardRow(
        teamId: Int,
        runs: List<RunInfo>,
        teamGroups: List<String>,
        problems: List<ProblemInfo>
    ): ScoreboardRow

    abstract val comparator : Comparator<ScoreboardRow>

    override fun getScoreboard(info: ContestInfo, runs: Map<Int, List<RunInfo>>): Scoreboard {
        logger.info("Calculating scoreboard: runs count = ${runs.values.sumOf { it.size }}")
        val teamsInfo = info.teams.filterNot { it.isHidden }.associateBy { it.id }
        fun ScoreboardRow.team() = teamsInfo[teamId]!!

        val hasChampion = mutableSetOf<String>()

        val rows = teamsInfo.values
            .map { info.getScoreboardRow(it.id, runs[it.id] ?: emptyList(), it.groups, info.problems) }
            .sortedWith(comparator.thenComparing { it -> it.team().name })
            .toMutableList()
        var right = 0
        var outOfContestTeams = 0
        while (right < rows.size) {
            val left = right
            while (right < rows.size && comparator.compare(rows[left], rows[right]) == 0) {
                right++
            }
            val minRank = left + 1 - outOfContestTeams
            val nextRank = minRank + rows.subList(left, right).count { !teamsInfo[it.teamId]!!.isOutOfContest }
            val medal = run {
                var skipped = 0
                for (type in info.medals) {
                    val canGetMedal = when (type.tiebreakMode) {
                        MedalTiebreakMode.ALL -> minRank <= type.count + skipped
                        MedalTiebreakMode.NONE -> nextRank <= type.count + skipped + 1
                    } && rows[left].totalScore >= type.minScore
                    if (canGetMedal) {
                        return@run type.name
                    }
                    skipped += type.count
                }
                null
            }
            for (i in left until right) {
                val team = teamsInfo[rows[i].teamId]!!
                if (team.isOutOfContest) {
                    outOfContestTeams++
                } else {
                    rows[i] = rows[i].copy(
                        rank = minRank,
                        medalType = medal,
                        championInGroups = team.groups.filter { it !in hasChampion }
                    )
                }
            }
            for (i in left until right) {
                hasChampion.addAll(teamsInfo[rows[i].teamId]!!.groups)
            }
        }
        return Scoreboard(runs.values.maxOfOrNull { it.maxOfOrNull { it.time } ?: Duration.ZERO } ?: Duration.ZERO , rows)
    }

    companion object {
        val logger = getLogger(AbstractScoreboardCalculator::class)
    }
}

fun getScoreboardCalculator(info: ContestInfo, optimismLevel: OptimismLevel) : ScoreboardCalculator = when (info.resultType) {
    ContestResultType.ICPC -> when (optimismLevel) {
        OptimismLevel.NORMAL -> ICPCNormalScoreboardCalculator()
        OptimismLevel.OPTIMISTIC -> ICPCOptimisticScoreboardCalculator()
        OptimismLevel.PESSIMISTIC -> ICPCPessimisticScoreboardCalculator()
    }
    ContestResultType.IOI -> IOIScoreboardCalculator()
}

fun Flow<ContestStateWithRunsByTeam>.calculateScoreboardWithInfo(optimismLevel: OptimismLevel) =
    filter { it.info != null }
    .conflate()
    .map {
        it.info!! to getScoreboardCalculator(it.info, optimismLevel).getScoreboard(it.info, it.runs)
    }


fun Flow<ContestStateWithRunsByTeam>.calculateScoreboard(optimismLevel: OptimismLevel) =
    calculateScoreboardWithInfo(optimismLevel).map { it.second }
