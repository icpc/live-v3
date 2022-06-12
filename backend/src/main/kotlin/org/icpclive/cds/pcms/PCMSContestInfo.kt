package org.icpclive.cds.pcms

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.api.toApi
import org.icpclive.cds.ProblemInfo
import kotlin.time.Duration

class PCMSContestInfo constructor(
    val problems: List<ProblemInfo>,
    val teams: Map<String, PCMSTeamInfo>,
    var startTime: Instant,
    var status: ContestStatus,
    val contestLength: Duration,
    val freezeTime: Duration
) {
    fun toApi() = org.icpclive.api.ContestInfo(
        status,
        startTime,
        contestLength,
        freezeTime,
        problems.map { it.toApi() },
        teams.values.map { it.toApi() }.sortedBy { it.id },
    )
}