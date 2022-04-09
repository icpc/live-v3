package org.icpclive.cds

import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.api.toApi
import org.icpclive.utils.getLogger
import org.icpclive.utils.humanReadable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

abstract class ContestInfo(
    private var startTime_: Instant,
    private var status_: ContestStatus,
) {
    abstract val problemsNumber: Int
    abstract val teamsNumber: Int
    var startTime
        get() = startTime_
        set(value) {
            if (startTime_ != value) logger.info("Set start time ${value.humanReadable}")
            startTime_ = value
        }
    abstract val problems: List<ProblemInfo>
    abstract val teams: List<TeamInfo>
    abstract val contestTime: Duration
    var status
        get() = status_
        set(value) {
            if (status_ != value) logger.info("New status: $value")
            status_ = value
        }


    abstract fun getParticipant(name: String): TeamInfo?
    abstract fun getParticipant(id: Int): TeamInfo?
    abstract fun getParticipantByHashTag(hashTag: String): TeamInfo?
    var contestLength = 5.hours
    var freezeTime = 4.hours

    companion object {
        val logger = getLogger(ContestInfo::class)
    }

    fun toApi() = org.icpclive.api.ContestInfo(
        status,
        startTime.toEpochMilliseconds(),
        contestLength.inWholeMilliseconds,
        freezeTime.inWholeMilliseconds,
        problems.map { it.toApi() },
        teams.map { it.toApi() }.sortedBy { it.id },
    )
}