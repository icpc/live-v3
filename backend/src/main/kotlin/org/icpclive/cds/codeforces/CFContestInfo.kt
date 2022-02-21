package org.icpclive.cds.codeforces

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


/**
 * @author egor@egork.net
 */
class CFContestInfo : ContestInfo(Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN) {
    override val problems = mutableListOf<CFProblemInfo>()
    override val teams: List<TeamInfo>
        get() = participantsById.values.toList()
    override val problemsNumber: Int
        get() = problemsMap.size
    override val teamsNumber: Int
        get() = cfStandings?.rows?.size ?: 0
    private var cfStandings: CFStandings? = null
    private val runsById: MutableMap<Int, CFRunInfo> = HashMap()
    private val runsByTeam: MutableMap<Int, List<MutableList<CFRunInfo>>> = HashMap()
    private val problemsMap: MutableMap<String, CFProblemInfo> = HashMap()
    private val participantsByName: MutableMap<String?, CFTeamInfo> = HashMap()
    private val participantsById: MutableMap<Int, CFTeamInfo> = HashMap()
    private var nextParticipantId = 1
    override fun getParticipant(name: String): CFTeamInfo? {
        return participantsByName[name]
    }

    override fun getParticipant(id: Int): CFTeamInfo? {
        return participantsById[id]
    }

    override fun getParticipantByHashTag(hashTag: String): CFTeamInfo? {
        return null
    }

    val runs: List<CFRunInfo>
        get() = runsById.values.sortedBy { it.time }

    override val contestTime: Duration
        get() {
            if (cfStandings == null) {
                return 0.seconds
            }
            return if (cfStandings!!.contest.relativeTimeSeconds == null) {
                0.seconds
            } else minOf(
                Clock.System.now() - startTime,
                cfStandings!!.contest.durationSeconds!!.seconds
            )
        }

    fun updateStandings(standings: CFStandings) {
        if (problemsMap.isEmpty() && standings.problems.isNotEmpty()) {
            for (problem in standings.problems) {
                val problemInfo = CFProblemInfo(problem, problemsNumber)
                problemsMap[problem.index] = problemInfo
                problems.add(problemInfo)
            }
        }
        this.cfStandings = standings
        contestLength = standings.contest.durationSeconds!!.seconds
        val phase = standings.contest.phase
        this.startTime = standings.contest.startTimeSeconds?.let { Instant.fromEpochSeconds(it) } ?: Instant.DISTANT_FUTURE
        status = when (phase) {
            CFContestPhase.BEFORE -> ContestStatus.BEFORE
            CFContestPhase.CODING -> ContestStatus.RUNNING
            else -> ContestStatus.OVER
        }
        for (row in standings.rows) {
            val teamInfo = CFTeamInfo(row)
            if (participantsByName.containsKey(teamInfo.name)) {
                teamInfo.id = participantsByName[teamInfo.name]!!.id
            } else {
                runsByTeam[nextParticipantId] = createEmptyRunsArray()
                teamInfo.id = nextParticipantId++
            }
            participantsByName[teamInfo.name] = teamInfo
            participantsById[teamInfo.id] = teamInfo
        }
    }
    fun updateSubmissions(submissions: List<CFSubmission?>) {
        for (submission in submissions.reversed()) {
            if (submission!!.author.participantType != CFPartyParticipantType.CONTESTANT) {
                continue
            }
            if (!participantsByName.containsKey(getName(submission.author))) {
                continue
            }
            if (runsById.containsKey(submission.id.toInt())) {
                val runInfo = runsById[submission.id.toInt()]
                runInfo!!.updateFrom(submission, (Clock.System.now() - startTime).inWholeMilliseconds)
            } else {
                val runInfo = CFRunInfo(submission)
                runsById[runInfo.id] = runInfo
                addRun(runInfo, runInfo.problemId)
            }
        }
    }

    private fun createEmptyRunsArray(): List<MutableList<CFRunInfo>> {
        val array: MutableList<MutableList<CFRunInfo>> = ArrayList(problemsMap.size)
        for (i in 0 until problemsMap.size) {
            array.add(ArrayList())
        }
        return array
    }

    fun getProblem(problem: CFProblem): CFProblemInfo? {
        return problemsMap[problem.index]
    }

    private fun addRun(run: CFRunInfo?, problem: Int) {
        val runs = getRuns(run!!.submission.author)[problem]
        runs.add(run)
        run.problem.update(run)
    }

    fun getRuns(party: CFParty): List<MutableList<CFRunInfo>> {
        return runsByTeam[participantsByName[getName(party)]!!.id]!!
    }

    companion object {
        fun getName(party: CFParty): String {
            return party.teamName ?: party.members[0].handle
        }
    }
}