package org.icpclive.events

import org.icpclive.events.EventsLoader.Companion.instance
import org.icpclive.api.ContestStatus
import java.util.*
import kotlin.math.min

abstract class ContestInfo {
    var teamsNumber = 0
    var problemsNumber = 0
    var startTime: Long = 0
        set(value) {
            System.err.println("Set start time " + Date(startTime))
            field = value
        }
    @JvmField
    var problems: MutableList<ProblemInfo> = ArrayList()
    @JvmField
    var lastTime: Long = 0
    var status = ContestStatus.BEFORE
        set(value) {
            System.err.println("New status: $value")
            lastTime = currentTime
            field = value
        }

    protected constructor() {}
    protected constructor(problemNumber: Int) {
        problemsNumber = problemNumber
    }


    open val timeFromStart: Long
        get() = ((System.currentTimeMillis() - startTime) * instance.emulationSpeed).toLong()
    val currentTime: Long
        get() = getCurrentTime(0)

    fun getCurrentTime(delay: Int): Long {
        return when (status) {
            ContestStatus.BEFORE -> 0
            ContestStatus.PAUSED -> lastTime
            ContestStatus.RUNNING -> if (startTime == 0L) 0 else Math.min(
                timeFromStart,
                CONTEST_LENGTH.toLong()
            )
            ContestStatus.OVER -> {
                if (delay != 0) {
                    min(
                        timeFromStart,
                        (CONTEST_LENGTH + delay).toLong()
                    )
                } else CONTEST_LENGTH.toLong()
            }
            else -> 0
        }
    }

    val isFrozen: Boolean
        get() = currentTime >= FREEZE_TIME

    abstract fun getParticipant(name: String?): TeamInfo?
    abstract fun getParticipant(id: Int): TeamInfo?
    abstract fun getParticipantByHashTag(hashTag: String?): TeamInfo?
    abstract val standings: Array<out TeamInfo>
    abstract fun getStandings(optimismLevel: OptimismLevel): Array<out TeamInfo>
    fun getStandings(group: String, optimismLevel: OptimismLevel): Array<out TeamInfo> {
        if (ALL_REGIONS == group) {
            return getStandings(optimismLevel)
        }
        val infos = getStandings(optimismLevel)
        return infos.filter { x: TeamInfo -> x.groups.contains(group) }.toTypedArray()
    }

    val hashTags by lazy {
        val hashtags = ArrayList<String>()
        val infos = standings
        for (teamInfo in infos) {
            teamInfo.hashTag?.run {
                hashtags.add(this)
            }
        }
        hashtags.toTypedArray()
    }

    abstract fun firstTimeSolved(): LongArray?
    abstract val firstSolvedRun: Array<out RunInfo?>
    abstract val runs: Array<out RunInfo?>
    abstract fun getRun(id: Int): RunInfo?
    abstract val lastRunId: Int

    companion object {
        @JvmField
        var CONTEST_LENGTH = 5 * 60 * 60 * 1000
        @JvmField
        var FREEZE_TIME = 4 * 60 * 60 * 1000
        @JvmField
        val GROUPS = TreeSet<String>()
        const val ALL_REGIONS = "all"
    }
}