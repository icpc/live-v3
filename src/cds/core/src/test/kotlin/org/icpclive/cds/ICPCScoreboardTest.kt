package org.icpclive.cds

import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes


class ICPCScoreboardTest {
    @Test
    fun testRanks() = TestData.run {
        val runs = listOf(
            RunInfo("1".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId4, 10.minutes, null),
            RunInfo("3".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId1, 30.minutes, null),
            RunInfo("4".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId3, 30.minutes, null),
            RunInfo("5".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId2, 40.minutes, null),
        )
        val calculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val scoreboardRows = runs.groupBy { it.teamId }.mapValues { calculator.getScoreboardRow(info, it.value) }
        val ranking = calculator.getRanking(info, scoreboardRows)
        assertEquals(ranking.ranks, listOf(1, 2, 2, 4))
        assertEquals(ranking.order, listOf(teamId4, teamId1, teamId3, teamId2))
    }

}