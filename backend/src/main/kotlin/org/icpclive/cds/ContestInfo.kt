package org.icpclive.cds

import humanReadable
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.api.ScoreboardRow
import org.icpclive.api.toApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

abstract class ContestInfo(
    private var startTime_: Instant,
    private var status_: ContestStatus,
){
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


    val isFrozen: Boolean
        get() = contestTime >= freezeTime

    abstract fun getParticipant(name: String): TeamInfo?
    abstract fun getParticipant(id: Int): TeamInfo?
    abstract fun getParticipantByHashTag(hashTag: String): TeamInfo?
    abstract fun getStandings(optimismLevel: OptimismLevel): List<ScoreboardRow>
    abstract fun firstTimeSolved(): LongArray?
    abstract val firstSolvedRun: List<RunInfo?>
    var contestLength = 5.hours
    var freezeTime = 4.hours
    val groups = mutableSetOf<String>()

    companion object {
        const val ALL_REGIONS = "all"
        val logger: Logger = LoggerFactory.getLogger(ContestInfo::class.java)
    }

    fun toApi() = org.icpclive.api.ContestInfo(
        status,
        startTime.toEpochMilliseconds(),
        contestLength.inWholeMilliseconds,
        freezeTime.inWholeMilliseconds,
        problems.map { it.toApi() },
        teams.map { it.toApi() }.sortedBy { it.id },
        EventsLoader.instance.emulationSpeed.toInt(),
    )
}