import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator
import kotlin.test.Test
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
        problemList = listOf(
            ProblemInfo("A", "A", 1, 0, "A"),
            ProblemInfo("B", "B", 2, 1, "B"),
        ),
        teamList = listOf(
            TeamInfo(1, "T1", "T1", "T1", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(2, "T2", "T2", "T2", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(3, "T3", "T3", "T3", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(4, "T4", "T4", "T4", emptyList(), null, emptyMap(), false, false, null),
        ),
        groupList = emptyList(),
        organizationList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    )

    @Test
    fun testRanks() {
        val runs = listOf(
            RunInfo(1, RunResult.ICPC(Verdict.Accepted, false), 1, 4, 10.minutes),
            RunInfo(3, RunResult.ICPC(Verdict.Accepted, false), 1, 1, 30.minutes),
            RunInfo(4, RunResult.ICPC(Verdict.Accepted, false), 1, 3, 30.minutes),
            RunInfo(5, RunResult.ICPC(Verdict.Accepted, false), 1, 2, 40.minutes),
        )
        val calculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val scoreboardRows = runs.groupBy { it.teamId }.mapValues { calculator.getScoreboardRow(info, it.value) }
        val ranking = calculator.getRanking(info, scoreboardRows)
        assertEquals(ranking.ranks, listOf(1, 2, 2, 4))
        assertEquals(ranking.order, listOf(4, 1, 3, 2))
    }

}