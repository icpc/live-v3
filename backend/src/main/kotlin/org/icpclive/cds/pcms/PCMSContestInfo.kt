package org.icpclive.cds.pcms

import kotlinx.datetime.Instant
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.ProblemInfo
import kotlin.time.Duration

class PCMSContestInfo(
    val problems: List<ProblemInfo>,
    val teams: Map<String, PCMSTeamInfo>,
    var startTime: Instant,
    var status: ContestStatus,
    val contestLength: Duration,
    val freezeTime: Duration
) {
    fun toApi() = ContestInfo(
        status,
        startTime,
        contestLength,
        freezeTime,
        problems,
        teams.values.map { it.teamInfo }.sortedBy { it.id },
    )
}