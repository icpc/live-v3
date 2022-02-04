package org.icpclive.cds

import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.ScoreboardRow
import kotlin.math.max

private fun TeamInfo.scoreboardRow(
    isAccepted: (run:RunInfo, index:Int, count:Int) -> Boolean,
    isAddingPenalty: (run:RunInfo, index:Int, count:Int) -> Boolean,
    isPending: (run:RunInfo, index:Int, count:Int) -> Boolean
): ScoreboardRow {
    var solved = 0
    var penalty = 0
    var lastAccepted = 0L
    val problemResults = runs.map { problemRuns ->
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
        id,
        0,
        solved,
        penalty,
        lastAccepted,
        problemResults
    )
}

fun TeamInfo.getTeamScoreboardRow(optimismLevel: OptimismLevel) =
    when (optimismLevel) {
        OptimismLevel.NORMAL -> scoreboardRow(
            { run, index, count -> run.isAccepted },
            { run, index, count -> run.isJudged && run.isAddingPenalty },
            { run, index, count -> !run.isJudged }
        )
        OptimismLevel.OPTIMISTIC -> scoreboardRow(
            { run, index, count -> run.isAccepted || (!run.isJudged && index == count - 1) },
            { run, index, count -> !run.isJudged || run.isAddingPenalty },
            { run, index, count -> false }
        )
        OptimismLevel.PESSIMISTIC -> scoreboardRow(
            { run, index, count -> run.isAccepted },
            { run, index, count -> !run.isJudged || run.isAddingPenalty },
            { run, index, count -> false }
        )
    }

fun sortAndSetRanks(results: MutableList<ScoreboardRow>, teams: List<TeamInfo>) {
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