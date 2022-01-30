package org.icpclive.cds

import org.icpclive.api.ICPCProblemResult
import org.icpclive.api.ScoreboardRow

interface TeamInfo {
    val id: Int
    val rank: Int
    val name: String
    val shortName: String
    val alias: String
    val groups: Set<String>
    val penalty: Int
    val solvedProblemsNumber: Int
    val lastAccepted: Long
    val runs: List<List<RunInfo>>
    fun addRun(run: RunInfo, problem: Int)
    val hashTag: String?
    fun copy(): TeamInfo

    val apiScoreboardRow
        get() = ScoreboardRow(
            id,
            rank,
            solvedProblemsNumber,
            penalty,
            apiProblemResults()
        )

    fun apiProblemResults() =
        runs.map { problemRuns ->
            val (runsBeforeFirstOk, okRun) = synchronized(problemRuns) {
                val okRunIndex = problemRuns.indexOfFirst { it.isAccepted }
                if (okRunIndex == -1) {
                    problemRuns to null
                } else {
                    problemRuns.toList().subList(0, okRunIndex) to problemRuns[okRunIndex]
                }
            }
            ICPCProblemResult(
                runsBeforeFirstOk.count { it.isAddingPenalty },
                runsBeforeFirstOk.count { !it.isJudged },
                okRun != null,
                okRun?.isFirstSolvedRun == true
            )
        }


    companion object {
        val comparator = compareBy<TeamInfo>(
            { -it.solvedProblemsNumber },
            { it.penalty },
            { it.lastAccepted }
        )
        val strictComparator: Comparator<TeamInfo> = comparator.thenComparing { it: TeamInfo -> it.name }
    }

    fun toApi() = org.icpclive.api.TeamInfo(
        id,
        rank,
        name,
        shortName,
        alias,
        groups.toList(),
        penalty,
        solvedProblemsNumber,
        lastAccepted,
        hashTag
    )

}