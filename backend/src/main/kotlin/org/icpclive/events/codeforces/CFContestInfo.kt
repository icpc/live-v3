package org.icpclive.events.codeforces

import org.icpclive.api.ContestStatus
import org.icpclive.events.ContestInfo
import org.icpclive.events.OptimismLevel
import org.icpclive.events.RunInfo
import org.icpclive.events.TeamInfo
import org.icpclive.events.codeforces.api.data.CFContest.CFContestPhase
import org.icpclive.events.codeforces.api.data.CFParty
import org.icpclive.events.codeforces.api.data.CFProblem
import org.icpclive.events.codeforces.api.data.CFSubmission
import org.icpclive.events.codeforces.api.results.CFStandings
import java.util.*


/**
 * @author egor@egork.net
 */
class CFContestInfo : ContestInfo() {
    private var cfStandings: CFStandings? = null
    private val runsById: MutableMap<Int, CFRunInfo> = HashMap()
    private val runsByTeam: MutableMap<Int, List<MutableList<CFRunInfo>>> = HashMap()
    private val problemsMap: MutableMap<String, CFProblemInfo> = HashMap()
    private val participantsByName: MutableMap<String?, CFTeamInfo> = HashMap()
    private val participantsById: MutableMap<Int, CFTeamInfo> = HashMap()
    override lateinit var firstSolvedRun: Array<CFRunInfo?>
    private var nextParticipantId = 1
    override fun getParticipant(name: String?): CFTeamInfo? {
        return participantsByName[name]
    }

    override fun getParticipant(id: Int): CFTeamInfo? {
        return participantsById[id]
    }

    override fun getParticipantByHashTag(hashTag: String?): CFTeamInfo? {
        return null
    }

    override val standings: Array<TeamInfo>
        get() = this.cfStandings?.rows?.map {
            participantsByName[getName(it.party)] as TeamInfo
        }?.toTypedArray() ?: emptyArray()

    override fun getStandings(optimismLevel: OptimismLevel): Array<TeamInfo> {
        return standings
    }

    override fun firstTimeSolved(): LongArray? {
        val result = LongArray(problemsMap.size)
        for (i in result.indices) {
            result[i] = if (firstSolvedRun[i] == null) 0 else firstSolvedRun[i]!!.time
        }
        return result
    }

    override val runs: Array<CFRunInfo>
        get() {
            synchronized(runsById) {
                val cfRunInfos = runsById.values.toTypedArray()
                Arrays.sort(cfRunInfos) { a: CFRunInfo, b: CFRunInfo -> java.lang.Long.compare(a.time, b.time) }
                return cfRunInfos
            }
        }

    override fun getRun(id: Int): RunInfo? {
        synchronized(runsById) { return runsById[id] }
    }

    override val lastRunId: Int
        get() = runsById.size - 1
    override val timeFromStart: Long
        get() {
            if (cfStandings == null) {
                return 0
            }
            return if (cfStandings!!.contest.relativeTimeSeconds == null) {
                0
            } else Math.min(
                System.currentTimeMillis() - startTime,
                cfStandings!!.contest.durationSeconds * 1000
            )
        }

    fun update(standings: CFStandings, submissions: List<CFSubmission?>?) {
        if (problemsMap.isEmpty() && !standings.problems.isEmpty()) {
            var id = 0
            for (problem in standings.problems) {
                val problemInfo = CFProblemInfo(problem, id++)
                problemsMap[problem.index] = problemInfo
                problems.add(problemInfo)
            }
            firstSolvedRun = arrayOfNulls(id)
            problemsNumber = id
        }
        this.cfStandings = standings
        //        lastTime = standings.contest.relativeTimeSeconds;
        CONTEST_LENGTH = standings.contest.durationSeconds.toInt() * 1000
        val phase = standings.contest.phase
        if (status === ContestStatus.BEFORE && phase == CFContestPhase.CODING) {
            this.startTime = System.currentTimeMillis() - standings.contest.relativeTimeSeconds * 1000
        }
        status =
            if (phase == CFContestPhase.BEFORE) ContestStatus.BEFORE else if (phase == CFContestPhase.CODING) ContestStatus.RUNNING else ContestStatus.OVER
        for (row in standings.rows) {
            val teamInfo = CFTeamInfo(row!!)
            if (participantsByName.containsKey(teamInfo.name)) {
                teamInfo.id = participantsByName[teamInfo.name]!!.id
            } else {
                runsByTeam[nextParticipantId] = createEmptyRunsArray()
                teamInfo.id = nextParticipantId++
            }
            participantsByName[teamInfo.name] = teamInfo
            participantsById[teamInfo.id] = teamInfo
        }
        teamsNumber = standings.rows.size
        if (submissions != null) {
            Collections.reverse(submissions)
            synchronized(runsById) {
                for (submission in submissions) {
                    if (submission!!.author.participantType != CFParty.CFPartyParticipantType.CONTESTANT || !participantsByName.containsKey(
                            getName(
                                submission.author
                            )
                        )
                    ) {
                        continue
                    }
                    var runInfo: CFRunInfo?
                    var isNew: Boolean
                    if (runsById.containsKey(submission.id.toInt())) {
                        runInfo = runsById[submission.id.toInt()]
                        runInfo!!.updateFrom(submission, standings.contest.relativeTimeSeconds)
                        isNew = false
                    } else {
                        runInfo = CFRunInfo(submission)
                        runsById[runInfo.id] = runInfo
                        isNew = true
                    }
                    if (isNew) {
                        addRun(runInfo, runInfo!!.problemId)
                    }
                    if (runInfo!!.isAccepted) {
                        val pid = runInfo.problemId
                        if (firstSolvedRun[pid] == null || firstSolvedRun[pid]!!.time > runInfo.time) {
                            firstSolvedRun[pid] = runInfo
                        }
                    }
                }
            }
        }
        for (row in standings.rows) {
            val teamInfo = CFTeamInfo(row)
            for (i in teamInfo.runs.indices) {
                for (runInfo in teamInfo.runs[i]) {
                    if (runInfo.points == 0) {
                        runInfo.points = row.problemResults[i].points.toInt()
                    }
                }
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

    fun addRun(run: CFRunInfo?, problem: Int) {
        val runs = getRuns(run!!.submission.author)[problem]
        synchronized(runs) {
            runs.add(run)
            run.problem.update(run)
        }
    }

    fun getRuns(party: CFParty): List<MutableList<CFRunInfo>> {
        return runsByTeam[participantsByName[getName(party)]!!.id]!!
    }

    companion object {
        fun getName(party: CFParty): String {
            return if (party.teamName == null) party.members[0].handle else party.teamName
        }
    }
}