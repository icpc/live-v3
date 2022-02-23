package org.icpclive.cds.codeforces

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestInfo
import org.icpclive.cds.ProblemInfo
import org.icpclive.cds.TeamInfo
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import java.awt.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


private val verdictToString: Map<CFSubmissionVerdict, String> = mapOf(
    CFSubmissionVerdict.CHALLENGED to "CH",
    CFSubmissionVerdict.COMPILATION_ERROR to "CE",
    CFSubmissionVerdict.CRASHED to "CR",
    CFSubmissionVerdict.FAILED to "FL",
    CFSubmissionVerdict.IDLENESS_LIMIT_EXCEEDED to "IL",
    CFSubmissionVerdict.INPUT_PREPARATION_CRASHED to "IC",
    CFSubmissionVerdict.MEMORY_LIMIT_EXCEEDED to "ML",
    CFSubmissionVerdict.OK to "AC",
    CFSubmissionVerdict.PARTIAL to "PA",
    CFSubmissionVerdict.PRESENTATION_ERROR to "PE",
    CFSubmissionVerdict.REJECTED to "RJ",
    CFSubmissionVerdict.RUNTIME_ERROR to "RE",
    CFSubmissionVerdict.SECURITY_VIOLATED to "SV",
    CFSubmissionVerdict.SKIPPED to "SK",
    CFSubmissionVerdict.TESTING to "",
    CFSubmissionVerdict.TIME_LIMIT_EXCEEDED to "TL",
    CFSubmissionVerdict.WRONG_ANSWER to "WA",
)



/**
 * @author egor@egork.net
 */
class CFContestInfo : ContestInfo(Instant.fromEpochMilliseconds(0), ContestStatus.UNKNOWN) {

    override val problems = mutableListOf<ProblemInfo>()
    override val teams: List<TeamInfo>
        get() = participantsById.values.toList()
    override val problemsNumber: Int
        get() = problemsMap.size
    override val teamsNumber: Int
        get() = cfStandings?.rows?.size ?: 0
    private var cfStandings: CFStandings? = null
    private val problemsMap = mutableMapOf<String, ProblemInfo>()
    private val participantsByName = mutableMapOf<String, CFTeamInfo>()
    private val participantsById = mutableMapOf<Int, CFTeamInfo>()
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
                val problemInfo = ProblemInfo(problem.index, problem.name!!, Color.BLACK)
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
                teamInfo.id = nextParticipantId++
            }
            participantsByName[teamInfo.name] = teamInfo
            participantsById[teamInfo.id] = teamInfo
        }
    }

    fun parseSubmissions(submissions: List<CFSubmission>) : List<RunInfo> {
        val problemTestsCount = submissions.groupingBy { it.problem.index }.fold(Int.MAX_VALUE) { acc, submit ->
            minOf(acc, submit.passedTestCount + if (submit.verdict == CFSubmissionVerdict.OK) 0 else 1)
        }
        return submissions.reversed().asSequence()
            .filter { it.author.participantType == CFPartyParticipantType.CONTESTANT }
            .filter { participantsByName.containsKey(getName(it.author)) }
            .map {
                val problemId = it.problem.index.toInt()
                val verdict = it.verdict ?: CFSubmissionVerdict.TESTING
                val problemTests = problemTestsCount[it.problem.index]!!
                RunInfo(
                    id = it.id.toInt(),
                    isAccepted = verdict == CFSubmissionVerdict.OK,
                    isAddingPenalty = verdict != CFSubmissionVerdict.TESTING &&
                            verdict != CFSubmissionVerdict.COMPILATION_ERROR &&
                            it.passedTestCount != 0,
                    isJudged = verdict != CFSubmissionVerdict.TESTING,
                    result = verdictToString[verdict]!!,
                    problemId = problemId,
                    teamId = getParticipant(getName(it.author))!!.id,
                    percentage = it.passedTestCount.toDouble() / problemTests,
                    time = it.relativeTimeSeconds * 1000,
                    lastUpdateTime = 0,
                    isFirstSolvedRun = false
                )
            }.toList()
    }



    companion object {
        fun getName(party: CFParty): String {
            return party.teamName ?: party.members[0].handle
        }
    }
}