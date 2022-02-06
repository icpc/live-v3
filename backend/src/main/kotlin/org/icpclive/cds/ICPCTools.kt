package org.icpclive.cds

import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.ScoreboardRow
import org.slf4j.LoggerFactory
import kotlin.math.max

object ICPCTools {

    val logger = LoggerFactory.getLogger(ICPCTools::class.java)

    private fun scoreboardRow(
        teamInfo: TeamInfo,
        isAccepted: (run:RunInfo, index:Int, count:Int) -> Boolean,
        isAddingPenalty: (run:RunInfo, index:Int, count:Int) -> Boolean,
        isPending: (run:RunInfo, index:Int, count:Int) -> Boolean
    ): ScoreboardRow {
        var solved = 0
        var penalty = 0
        var lastAccepted = 0L
        val problemResults = teamInfo.runs.map { problemRuns ->
            val (runsBeforeFirstOk, okRun) = run {
                val okRunIndex = problemRuns.withIndex().indexOfFirst { isAccepted(it.value, it.index, problemRuns.size) }
                if (okRunIndex == -1) {
                    problemRuns to null
                } else {
                    problemRuns.toList().subList(0, okRunIndex) to problemRuns[okRunIndex]
                }
            }
            ICPCProblemResult(
                runsBeforeFirstOk.withIndex().count { isAddingPenalty(it.value, it.index, problemRuns.size) },
                runsBeforeFirstOk.withIndex().count { isPending(it.value, it.index, problemRuns.size) },
                okRun != null,
                okRun?.isFirstSolvedRun == true,
            ).also {
                if (it.isSolved) {
                    solved++
                    penalty += (okRun!!.time / 1000 / 60).toInt()
                    lastAccepted = max(lastAccepted, okRun.time)
                }
            }
        }
        return ScoreboardRow(
            teamInfo.id,
            0,
            solved,
            penalty,
            lastAccepted,
            problemResults
        )
    }

    fun getTeamScoreboardRow(teamInfo: TeamInfo, optimismLevel: OptimismLevel) =
        when (optimismLevel) {
            OptimismLevel.NORMAL -> scoreboardRow(
                teamInfo,
                { run, _, _ -> run.isAccepted },
                { run, _, _ -> run.isJudged && run.isAddingPenalty },
                { run, _, _ -> !run.isJudged }
            )
            OptimismLevel.OPTIMISTIC -> scoreboardRow(
                teamInfo,
                { run, index, count -> run.isAccepted || (!run.isJudged && index == count - 1) },
                { run, _, _ -> !run.isJudged || run.isAddingPenalty },
                { _, _, _ -> false }
            )
            OptimismLevel.PESSIMISTIC -> scoreboardRow(
                teamInfo,
                { run, _, _ -> run.isAccepted },
                { run, _, _ -> !run.isJudged || run.isAddingPenalty },
                { _, _, _ -> false }
            )
        }

    private fun sortAndSetRanks(results: MutableList<ScoreboardRow>, teams: List<TeamInfo>) {
        val comparator = compareBy<ScoreboardRow>(
            { -it.totalScore },
            { it.penalty },
            { it.lastAccepted }
        )
        results.sortWith(
            comparator.thenComparing { it:ScoreboardRow ->
                teams[it.teamId].name
            }
        )
        var rank = 1
        results[0] = results[0].copy(rank = 1)
        for (i in 1 until results.size) {
            if (comparator.compare(results[i-1], results[i]) < 0) {
                rank++
            }
            results[i] = results[i].copy(rank = rank)
        }
    }

    fun getScoreboard(teams: List<TeamInfo>, optimismLevel: OptimismLevel) =
        MutableList(teams.size) { getTeamScoreboardRow(teams[it], optimismLevel) }
            .apply { sortAndSetRanks(this, teams) }
            .also {
                if (optimismLevel == OptimismLevel.NORMAL) {
                   for (row in it) {
                        teams[row.teamId].cdsScoreboardRow?.run {
                            if (this != row) {
                                logger.warn(
                                    """|Team scoreboard record mismatch:
                                       |Computed: $row
                                       |From cds: $this
                                    """.trimMargin("|")
                                )
                            }
                        }

                    }
                }
            }
            .toList()

}