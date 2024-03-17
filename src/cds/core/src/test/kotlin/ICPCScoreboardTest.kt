import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ICPCScoreboardTest {
    val problemIdA = ProblemId("A")
    val problemIdB = ProblemId("B")
    val info = ContestInfo(
        name = "",
        status = ContestStatus.OVER,
        resultType = ContestResultType.ICPC,
        startTime = Instant.fromEpochMilliseconds(1687460000),
        contestLength = 5.hours,
        freezeTime = 4.hours,
        problemList = listOf(
            ProblemInfo(problemIdA, "A", "A", 1),
            ProblemInfo(problemIdB, "B", "B", 2),
        ),
        teamList = listOf(
            TeamInfo(TeamId("T1"), "T1", "T1", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(TeamId("T2"), "T2", "T2", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(TeamId("T3"), "T3", "T3", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(TeamId("T4"), "T4", "T4", emptyList(), null, emptyMap(), false, false, null),
        ),
        groupList = emptyList(),
        organizationList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    )

    @Test
    fun testRanks() {
        val runs = listOf(
            RunInfo(RunId("1"), RunResult.ICPC(Verdict.Accepted, false), problemIdA, TeamId("T4"), 10.minutes),
            RunInfo(RunId("3"), RunResult.ICPC(Verdict.Accepted, false), problemIdA, TeamId("T1"), 30.minutes),
            RunInfo(RunId("4"), RunResult.ICPC(Verdict.Accepted, false), problemIdA, TeamId("T3"), 30.minutes),
            RunInfo(RunId("5"), RunResult.ICPC(Verdict.Accepted, false), problemIdA, TeamId("T2"), 40.minutes),
        )
        val calculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val scoreboardRows = runs.groupBy { it.teamId }.mapValues { calculator.getScoreboardRow(info, it.value) }
        val ranking = calculator.getRanking(info, scoreboardRows)
        assertEquals(ranking.ranks, listOf(1, 2, 2, 4))
        assertEquals(ranking.order, listOf(TeamId("T4"), TeamId("T1"), TeamId("T3"), TeamId("T2")))
    }

}