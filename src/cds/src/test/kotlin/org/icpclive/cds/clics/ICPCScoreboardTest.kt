package org.icpclive.cds.clics

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.scoreboard.getScoreboardCalculator
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ICPCScoreboardTest {
    val info = ContestInfo(
        name = "",
        status = ContestStatus.OVER,
        resultType = ContestResultType.ICPC,
        startTime = Instant.fromEpochMilliseconds(1687460000),
        contestLength = 5.hours,
        freezeTime = 4.hours,
        problems = listOf(
            ProblemInfo("A", "A", 1, 0, "A"),
            ProblemInfo("B", "B", 2, 1, "B"),
        ),
        teams = listOf(
            TeamInfo(1, "T1", "T1", "T1", emptyList(), null, emptyMap()),
            TeamInfo(2, "T2", "T2", "T2", emptyList(), null, emptyMap()),
            TeamInfo(3, "T3", "T3", "T3", emptyList(), null, emptyMap()),
            TeamInfo(4, "T4", "T4", "T4", emptyList(), null, emptyMap()),
        ),
        groups = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    )

    @Test
    fun testRanks() {
        val runs = listOf(
            RunInfo(1, ICPCRunResult(Verdict.Accepted, false), 1.0, 1, 4, 10.minutes),
            RunInfo(3, ICPCRunResult(Verdict.Accepted, false), 1.0, 1, 1, 30.minutes),
            RunInfo(4, ICPCRunResult(Verdict.Accepted, false), 1.0, 1, 3, 30.minutes),
            RunInfo(5, ICPCRunResult(Verdict.Accepted, false), 1.0, 1, 2, 40.minutes),
        )
        val scoreboard = getScoreboardCalculator(info, OptimismLevel.NORMAL).getScoreboard(info, runs.groupBy { it.teamId })
        assertEquals(scoreboard.rows.map { it.rank }, listOf(1, 2, 2, 4))
        assertEquals(scoreboard.rows.map { it.teamId }, listOf(4, 1, 3, 2))
    }

}