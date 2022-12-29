package org.icpclive.cds.codeforces

import CFHack
import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.util.getLogger
import kotlin.math.ceil
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
    private val participantsByName = mutableMapOf<String, CFTeamInfo>()
    private val participantsById = mutableMapOf<Int, CFTeamInfo>()
    private var nextParticipantId = 1
    private var contestType: CFContestType = CFContestType.ICPC

    fun updateStandings(standings: CFStandings) {
        this.cfStandings = standings
        contestType = standings.contest.type
        contestLength = standings.contest.durationSeconds!!
        if (problemsMap.isEmpty() && standings.problems.isNotEmpty()) {
            for ((id, problem) in standings.problems.withIndex()) {
                val problemInfo = ProblemInfo(
                    letter = problem.index,
                    name = problem.name!!,
                    id = id,
                    ordinal = id,
                    minScore = if (problem.points != null) 0.0 else null,
                    maxScore = problem.points,
                    scoreMergeMode = when (contestType) {
                        CFContestType.CF -> ScoreMergeMode.LAST_OK
                        CFContestType.ICPC -> null
                        CFContestType.IOI -> ScoreMergeMode.MAX_TOTAL
                    }
                )
                problemsMap[problem.index] = problemInfo
                problems.add(problemInfo)
            }
            if (contestType == CFContestType.CF) {
                val hacksInfo = ProblemInfo(
                    letter = "*",
                    name = "Hacks",
                    id = -1,
                    ordinal = -1,
                    minScore = null,
                    maxScore = null,
                    scoreMergeMode = ScoreMergeMode.SUM,
                )
                problemsMap[hacksInfo.letter] = hacksInfo
                problems.add(hacksInfo)
            }
        }
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

    private val CFSubmission.isAddingPenalty
        get() = verdict != CFSubmissionVerdict.OK && verdict != CFSubmissionVerdict.COMPILATION_ERROR && passedTestCount != 0

    private fun submissionToResult(submission: CFSubmission, wrongAttempts: Int) : RunResult? {
        if (submission.verdict == null || submission.verdict == CFSubmissionVerdict.TESTING) return null
        return when (contestType) {
            CFContestType.ICPC -> {
                ICPCRunResult(
                    isAccepted = submission.verdict == CFSubmissionVerdict.OK,
                    isAddingPenalty = submission.isAddingPenalty,
                    result = verdictToString[submission.verdict]!!,
                    isFirstToSolveRun = false
                )
            }

            CFContestType.IOI -> TODO()
            CFContestType.CF -> {
                // TODO: this should come from API
                val maxScore = submission.problem.points!!
                val isWrong = submission.verdict !in listOf(CFSubmissionVerdict.OK, CFSubmissionVerdict.CHALLENGED, CFSubmissionVerdict.SKIPPED)
                val score = if (!isWrong) {
                    maxOf(
                        maxScore * 3 / 10,
                        ceil(maxScore - submission.relativeTimeSeconds.inWholeMinutes * (maxScore / 250.0 * 120.0 / contestLength.inWholeMinutes) - 50 * wrongAttempts)
                    )
                } else {
                    0.0
                }
                IOIRunResult(
                    score = score,
                    difference = 0.0,
                    scoreByGroup = listOf(score),
                    wrongVerdict = if (isWrong) verdictToString[submission.verdict] else null
                )
            }
        }
    }

    fun parseSubmissions(submissions: List<CFSubmission>): List<RunInfo> {
        val problemTestsCount = submissions.groupingBy { it.problem.index }.fold(Int.MAX_VALUE) { acc, submit ->
            minOf(acc, submit.passedTestCount + if (submit.verdict == CFSubmissionVerdict.OK) 0 else 1)
        }
        return submissions.reversed().asSequence()
            .filter { it.author.participantType == CFPartyParticipantType.CONTESTANT }
            .filter { participantsByName.containsKey(getName(it.author)) }
            .groupBy { it.author to it.problem }
            .mapValues {(_, submissions) ->
                var wrongs = 0
                submissions.sortedBy { it.id }.map {
                    val problemId = problemsMap[it.problem.index]!!.id
                    val problemTests = problemTestsCount[it.problem.index]!!
                    wrongs += if (it.isAddingPenalty) 1 else 0
                    RunInfo(
                        id = it.id.toInt(),
                        submissionToResult(it, wrongs),
                        problemId = problemId,
                        teamId = participantsByName[getName(it.author)]!!.id,
                        percentage = it.passedTestCount.toDouble() / problemTests,
                        time = it.relativeTimeSeconds,
                    )
                }
            }.values.flatten()
    }

    fun parseHacks(hacks: List<CFHack>) : List<RunInfo> {
        return buildList {
            for (hack in hacks) {
                if (hack.hacker.participantType != CFPartyParticipantType.CONTESTANT) continue
                add(
                    RunInfo(
                        id = (hack.id * 2).inv(),
                        result = when (hack.verdict) {
                            CFHackVerdict.HACK_SUCCESSFUL -> IOIRunResult(
                                score = 100.0,
                                difference = 0.0,
                                scoreByGroup = emptyList(),
                                wrongVerdict = null
                            )

                            CFHackVerdict.HACK_UNSUCCESSFUL -> IOIRunResult(
                                score = -50.0,
                                difference = 0.0,
                                scoreByGroup = emptyList(),
                                wrongVerdict = null
                            )

                            CFHackVerdict.TESTING -> null
                            else -> IOIRunResult(
                                score = 0.0,
                                difference = 0.0,
                                scoreByGroup = emptyList(),
                                wrongVerdict = "CE"
                            )
                        },
                        percentage = 0.0,
                        problemId = -1,
                        teamId = participantsByName[getName(hack.hacker)]!!.id,
                        time = hack.creationTimeSeconds - startTime
                    )
                )
                add(
                    RunInfo(
                        id = (hack.id * 2 + 1).inv(),
                        result = IOIRunResult(
                            score = 0.0,
                            difference = 0.0,
                            scoreByGroup = emptyList(),
                            wrongVerdict = if (hack.verdict == CFHackVerdict.HACK_SUCCESSFUL) null else "OK"
                        ),
                        isHidden = hack.verdict != CFHackVerdict.HACK_SUCCESSFUL,
                        percentage = 0.0,
                        problemId = problemsMap[hack.problem.index]!!.id,
                        teamId = participantsByName[getName(hack.defender)]!!.id,
                        time = hack.creationTimeSeconds - startTime
                    )
                )
            }
        }
    }

    fun toApi() = ContestInfo(
        status,
        when (contestType) {
            CFContestType.CF -> ContestResultType.IOI
            CFContestType.IOI -> ContestResultType.IOI
            CFContestType.ICPC -> ContestResultType.ICPC
        },
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