package org.icpclive.cds.yandex

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.ProblemInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.yandex.api.ContestDescription
import org.icpclive.cds.yandex.api.Participant
import org.icpclive.cds.yandex.api.Problem
import kotlin.time.Duration

class YandexContestInfo(
    startTime: Instant,
    durationMs: Long,
    override val problems: List<ProblemInfo>,
    override val teams: List<TeamInfo>
): ContestInfo(
    startTime,
    deduceStatus(startTime, durationMs)
) {
    constructor(
        contestDescription: ContestDescription,
        problems: List<Problem>,
        participants: List<Participant>
    ) : this(
        Instant.parse(contestDescription.startTime),
        contestDescription.duration * 1000,
        problems.map(Problem::toProblemInfo),
        participants.map(Participant::toTeamInfo)
    )

    override val problemsNumber: Int
        get() = problems.size

    override val teamsNumber: Int
        get() = teams.size

    override val contestTime: Duration = Clock.System.now() - startTime

    override fun getParticipant(name: String): TeamInfo? {
        return teams.firstOrNull { it.name == name }
    }

    override fun getParticipant(id: Int): TeamInfo? {
        return teams.firstOrNull { it.id == id }
    }

    override fun getParticipantByHashTag(hashTag: String): TeamInfo? {
        return null
    }

    fun
}

// There is no way to fetch YC server time, so here we go
fun deduceStatus(startTime: Instant, durationMs: Long): ContestStatus {
    val now = Clock.System.now()
    val endTime = startTime.plus(durationMs, DateTimeUnit.MILLISECOND)

    return when {
        now < startTime -> ContestStatus.BEFORE
        now < endTime -> ContestStatus.RUNNING
        else -> ContestStatus.OVER
    }
}