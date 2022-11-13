package org.icpclive.cds.codeforces

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
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

class CFContestInfo {
    private var contestLength: Duration = 5.hours
    private var startTime: Instant = Instant.fromEpochMilliseconds(0)
    private var status = ContestStatus.BEFORE
    private val problems = mutableListOf<ProblemInfo>()
    private var cfStandings: CFStandings? = null
    private val problemsMap = mutableMapOf<String, ProblemInfo>()
    private val problemsIdMap = mutableMapOf<String, Int>()
    private val participantsByName = mutableMapOf<String, CFTeamInfo>()
    private val participantsById = mutableMapOf<Int, CFTeamInfo>()
    private var nextParticipantId = 1

    fun updateStandings(standings: CFStandings) {
        if (problemsMap.isEmpty() && standings.problems.isNotEmpty()) {
            for ((id, problem) in standings.problems.withIndex()) {
                val problemInfo = ProblemInfo(problem.index, problem.name!!, null, id, id)
                problemsMap[problem.index] = problemInfo
                problemsIdMap[problem.index] = id
                problems.add(problemInfo)
            }
        }
        this.cfStandings = standings
        contestLength = standings.contest.durationSeconds!!.seconds
        val phase = standings.contest.phase
        startTime = standings.contest.startTimeSeconds
            ?.let { Instant.fromEpochSeconds(it) }
            ?: Instant.DISTANT_FUTURE
        status = when (phase) {
            CFContestPhase.BEFORE -> ContestStatus.BEFORE
            CFContestPhase.CODING -> ContestStatus.RUNNING
            else -> ContestStatus.OVER
        }
        for (row in standings.rows) {
            val teamInfo = CFTeamInfo(row)
            if (participantsByName.containsKey(getName(row.party))) {
                teamInfo.id = participantsByName[getName(row.party)]!!.id
            } else {
                teamInfo.id = nextParticipantId++
            }
            participantsByName[getName(row.party)] = teamInfo
            participantsById[teamInfo.id] = teamInfo
        }
    }

    fun parseSubmissions(submissions: List<CFSubmission>): List<RunInfo> {
        val problemTestsCount = submissions.groupingBy { it.problem.index }.fold(Int.MAX_VALUE) { acc, submit ->
            minOf(acc, submit.passedTestCount + if (submit.verdict == CFSubmissionVerdict.OK) 0 else 1)
        }
        return submissions.reversed().asSequence()
            .filter { it.author.participantType == CFPartyParticipantType.CONTESTANT }
            .filter { participantsByName.containsKey(getName(it.author)) }
            .map {
                val problemId = problemsIdMap[it.problem.index]!!
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
                    teamId = participantsByName[getName(it.author)]!!.id,
                    percentage = it.passedTestCount.toDouble() / problemTests,
                    time = it.relativeTimeSeconds.seconds,
                )
            }.toList()
    }

    fun toApi() = ContestInfo(
        status,
        ContestResultType.ICPC,
        startTime,
        contestLength,
        0.seconds,
        problems,
        participantsById.values.map { it.toApi() }.sortedBy { it.id },
    )

    companion object {
        fun getName(party: CFParty): String {
            return party.teamName ?: party.members[0].handle
        }
    }
}