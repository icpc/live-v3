package org.icpclive.cds.pcms

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.*
import kotlin.time.Duration.Companion.seconds

class PCMSContestInfo(
    override val problems: List<ProblemInfo>,
    override val teams: List<PCMSTeamInfo>,
    startTime: Instant,
    status: ContestStatus,
    ) : ContestInfo(startTime, status) {
    override val problemsNumber: Int
        get() = problems.size
    override var contestTime = 0.seconds


    override val teamsNumber
        get() = teams.size

    override fun getParticipant(name: String): PCMSTeamInfo? {
        return teams.firstOrNull { it.alias == name }
    }

    override fun getParticipant(id: Int): PCMSTeamInfo? {
        return teams.firstOrNull { it.id == id }
    }

    override fun getParticipantByHashTag(hashTag: String): PCMSTeamInfo? {
        return teams.firstOrNull { hashTag.equals(it.hashTag, ignoreCase = true)}
    }
}