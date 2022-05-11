package org.icpclive.cds.clics.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.TeamInfo
import kotlin.time.Duration

open class ClicsContestInfo(
    problemsMap: Map<String, ClicsProblemInfo>,
    final override val teams: List<TeamInfo>,
    startTime: Instant,
    override var contestLength: Duration,
    override var freezeTime: Duration,
    status: ContestStatus
) : ContestInfo(startTime, status) {
    private val participantsByName = teams.groupBy { it.name }.mapValues { it.value.first() }
    override val problemsNumber: Int = problemsMap.size
    override val teamsNumber: Int = teams.size
    override val problems: List<ClicsProblemInfo> = problemsMap.values.toList()
    override val contestTime: Duration
        get() = minOf(Clock.System.now() - startTime, contestLength)
    // todo: fix this

    override fun getParticipant(name: String) = participantsByName[name]
    private val participantsById = teams.groupBy { it.id }.mapValues { it.value.first() }
    override fun getParticipant(id: Int) = participantsById[id]
    private val participantsByHashtag = teams.groupBy { it.hashTag }.mapValues { it.value.first() }
    override fun getParticipantByHashTag(hashTag: String) = participantsByHashtag[hashTag]
}
