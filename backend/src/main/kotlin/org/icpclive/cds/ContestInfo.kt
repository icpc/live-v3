package org.icpclive.cds

import org.icpclive.api.ContestStatus
import org.icpclive.api.ScoreboardRow
import org.icpclive.api.toApi
import org.icpclive.cds.EventsLoader.Companion.instance
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.min

abstract class ContestInfo{
    abstract val problemsNumber: Int
    abstract val teamsNumber: Int
    var startTime: Long = 0
        set(value) {
            logger.info("Set start time " + Date(startTime))
            field = value
        }
    abstract val problems: List<ProblemInfo>
    abstract val teams: List<TeamInfo>
    var lastTime: Long = 0
    var status = ContestStatus.BEFORE
        set(value) {
            if (field != value) logger.info("New status: $value")
            lastTime = currentTime
            field = value
        }


    open val timeFromStart: Long
        get() = ((System.currentTimeMillis() - startTime) * instance.emulationSpeed).toLong()
    val currentTime: Long
        get() = getCurrentTime(0)

    private fun getCurrentTime(delay: Int): Long {
        return when (status) {
            ContestStatus.BEFORE -> 0
            ContestStatus.PAUSED -> lastTime
            ContestStatus.RUNNING -> if (startTime == 0L) 0 else min(
                timeFromStart,
                contestLength.toLong()
            )
            ContestStatus.OVER -> {
                if (delay != 0) {
                    min(
                        timeFromStart,
                        (contestLength + delay).toLong()
                    )
                } else contestLength.toLong()
            }
            else -> 0
        }
    }

    val isFrozen: Boolean
        get() = currentTime >= freezeTime

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
        teams.map { it.toApi() }.sortedBy { it.id }
    )
}