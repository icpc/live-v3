package org.icpclive.cds

import org.icpclive.api.ContestStatus
import org.icpclive.api.ScoreboardRow
import org.icpclive.api.toApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

abstract class ContestInfo{
    abstract val problemsNumber: Int
    abstract val teamsNumber: Int
    var startTime: Long = 0
        set(value) {
            logger.info("Set start time " + Date(value))
            field = value
        }
    abstract val problems: List<ProblemInfo>
    abstract val teams: List<TeamInfo>
    abstract val contestTime: Long
    var status = ContestStatus.BEFORE
        set(value) {
            if (field != value) logger.info("New status: $value")
            field = value
        }


    val isFrozen: Boolean
        get() = contestTime >= freezeTime

    abstract fun getParticipant(name: String): TeamInfo?
    abstract fun getParticipant(id: Int): TeamInfo?
    abstract fun getParticipantByHashTag(hashTag: String): TeamInfo?
    abstract fun getStandings(optimismLevel: OptimismLevel): List<ScoreboardRow>
    abstract fun firstTimeSolved(): LongArray?
    abstract val firstSolvedRun: List<RunInfo?>
    abstract val runs: List<RunInfo>
    var contestLength = 5 * 60 * 60 * 1000
    var freezeTime = 4 * 60 * 60 * 1000
    val groups = mutableSetOf<String>()

    companion object {
        const val ALL_REGIONS = "all"
        val logger: Logger = LoggerFactory.getLogger(ContestInfo::class.java)
    }

    fun toApi() = org.icpclive.api.ContestInfo(
        status,
        startTime,
        contestLength.toLong(),
        freezeTime.toLong(),
        problems.map { it.toApi() },
        teams.map { it.toApi() }.sortedBy { it.id },
        EventsLoader.instance.emulationSpeed.toInt(),
    )
}