package org.icpclive.cds

import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.api.PenaltyRoundingMode
import org.icpclive.cds.api.ProblemInfo
import org.icpclive.cds.api.TeamInfo
import org.icpclive.cds.api.toProblemId
import org.icpclive.cds.api.toTeamId
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

object TestData {
    val problemIdA = "A".toProblemId()
    val problemIdB = "B".toProblemId()
    val teamId1 = "T1".toTeamId()
    val teamId2 = "T2".toTeamId()
    val teamId3 = "T3".toTeamId()
    val teamId4 = "T4".toTeamId()

    val startTime = Instant.fromEpochMilliseconds(1687460000)

    val info = ContestInfo(
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
        languagesList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    )


}