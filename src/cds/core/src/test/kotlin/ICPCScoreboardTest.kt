import kotlinx.datetime.Instant
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.getScoreboardCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class ICPCScoreboardTest {
    private val problemIdA = "A".toProblemId()
    private val problemIdB = "B".toProblemId()
    private val teamId1 = "T1".toTeamId()
    private val teamId2 = "T2".toTeamId()
    private val teamId3 = "T3".toTeamId()
    private val teamId4 = "T4".toTeamId()

    private val startTime = Instant.fromEpochMilliseconds(1687460000)

    private val info = ContestInfo(
        name = "",
        status = ContestStatus.OVER(startedAt = startTime, frozenAt = startTime + 4.hours, finishedAt = startTime + 5.hours),
        resultType = ContestResultType.ICPC,
        contestLength = 5.hours,
        freezeTime = 4.hours,
        problemList = listOf(
            ProblemInfo(problemIdA, "A", "A", 1),
            ProblemInfo(problemIdB, "B", "B", 2),
        ),
        teamList = listOf(
            TeamInfo(teamId1, "T1", "T1", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(teamId2, "T2", "T2", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(teamId3, "T3", "T3", emptyList(), null, emptyMap(), false, false, null),
            TeamInfo(teamId4, "T4", "T4", emptyList(), null, emptyMap(), false, false, null),
        ),
        groupList = emptyList(),
        organizationList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    )

    @Test
    fun testRanks() {
        val runs = listOf(
            RunInfo("1".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId4, 10.minutes),
            RunInfo("3".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId1, 30.minutes),
            RunInfo("4".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId3, 30.minutes),
            RunInfo("5".toRunId(), RunResult.ICPC(Verdict.Accepted, false), problemIdA, teamId2, 40.minutes),
        )
        val calculator = getScoreboardCalculator(info, OptimismLevel.NORMAL)
        val scoreboardRows = runs.groupBy { it.teamId }.mapValues { calculator.getScoreboardRow(info, it.value) }
        val ranking = calculator.getRanking(info, scoreboardRows)
        assertEquals(ranking.ranks, listOf(1, 2, 2, 4))
        assertEquals(ranking.order, listOf(teamId4, teamId1, teamId3, teamId2))
    }

}