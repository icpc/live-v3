package org.icpclive.cds.codeforces

import kotlinx.datetime.Instant
import org.icpclive.api.*
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours


internal class CFContestInfo {
    private var contestLength: Duration = 5.hours
    private var startTime: Instant = Instant.fromEpochMilliseconds(0)
    var status = ContestStatus.BEFORE
        private set
    private val problems = mutableListOf<ProblemInfo>()
    private var cfStandings: CFStandings? = null
    private val problemsMap = mutableMapOf<String, ProblemInfo>()
    private val participantsByCdsId = mutableMapOf<String, TeamInfo>()
    private var nextParticipantId = 1
    private var contestType: CFContestType = CFContestType.ICPC
    private var name: String = ""

    private fun updateContestInfo(contest: CFContest) {
        name = contest.name
        contestType = contest.type
        contestLength = contest.duration!!
        startTime = contest.startTime
            ?: Instant.DISTANT_FUTURE
        status = when (contest.phase) {
            CFContestPhase.BEFORE -> ContestStatus.BEFORE
            CFContestPhase.CODING -> ContestStatus.RUNNING
            else -> ContestStatus.OVER
        }
    }

    fun updateStandings(standings: CFStandings) {
        this.cfStandings = standings
        updateContestInfo(standings.contest)
        if (problemsMap.isEmpty() && standings.problems.isNotEmpty()) {
            for ((id, problem) in standings.problems.withIndex()) {
                val problemInfo = ProblemInfo(
                    letter = problem.index,
                    name = problem.name!!,
                    id = id,
                    ordinal = id,
                    contestSystemId = id.toString(),
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
                    contestSystemId = "hacks",
                    minScore = null,
                    maxScore = null,
                    scoreMergeMode = ScoreMergeMode.SUM,
                )
                problemsMap[hacksInfo.letter] = hacksInfo
                problems.add(hacksInfo)
            }
        }
        for (row in standings.rows) {
            val cdsId = getTeamCdsId(row.party)
            val id = participantsByCdsId[cdsId]?.id ?: nextParticipantId++
            val party = row.party
            participantsByCdsId[cdsId] = TeamInfo(
                id = id,
                fullName = party.teamName ?: party.members[0].let { it.name ?: it.handle },
                displayName = party.teamName ?: party.members[0].let { it.name ?: it.handle },
                contestSystemId = cdsId,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                organizationId = null,
                isHidden = false,
                isOutOfContest = false
            )
        }
    }

    private val CFSubmission.processedVerdict get() = when (verdict) {
        CFSubmissionVerdict.FAILED -> Verdict.Fail
        CFSubmissionVerdict.OK -> Verdict.Accepted
        CFSubmissionVerdict.PARTIAL -> null
        CFSubmissionVerdict.COMPILATION_ERROR -> Verdict.CompilationError
        CFSubmissionVerdict.RUNTIME_ERROR -> Verdict.RuntimeError
        CFSubmissionVerdict.WRONG_ANSWER -> Verdict.WrongAnswer
        CFSubmissionVerdict.PRESENTATION_ERROR -> Verdict.PresentationError
        CFSubmissionVerdict.TIME_LIMIT_EXCEEDED -> Verdict.TimeLimitExceeded
        CFSubmissionVerdict.MEMORY_LIMIT_EXCEEDED -> Verdict.MemoryLimitExceeded
        CFSubmissionVerdict.IDLENESS_LIMIT_EXCEEDED -> Verdict.IdlenessLimitExceeded
        CFSubmissionVerdict.SECURITY_VIOLATED -> Verdict.SecurityViolation
        CFSubmissionVerdict.CRASHED -> Verdict.Fail
        CFSubmissionVerdict.INPUT_PREPARATION_CRASHED -> Verdict.Fail
        CFSubmissionVerdict.CHALLENGED -> Verdict.Challenged
        CFSubmissionVerdict.SKIPPED -> Verdict.Ignored
        CFSubmissionVerdict.TESTING -> null
        CFSubmissionVerdict.REJECTED -> Verdict.Rejected
        null -> null
    }?.let {
        if (contestType == CFContestType.CF && passedTestCount == 0 && it.isAddingPenalty && !it.isAccepted) {
            Verdict.lookup(it.shortName, isAddingPenalty = false, isAccepted = false)
        } else {
            it
        }
    }

    private fun submissionToResult(submission: CFSubmission, wrongAttempts: Int) : RunResult? {
        if (submission.verdict == null || submission.verdict == CFSubmissionVerdict.TESTING) return null
        return when (contestType) {
            CFContestType.ICPC -> {
                submission.processedVerdict?.toRunResult()
            }

            CFContestType.IOI -> {
                // TODO: is CFSubmissionVerdict.SKIPPED not wrong
                val isWrong = submission.verdict !in listOf(CFSubmissionVerdict.OK, CFSubmissionVerdict.CHALLENGED, CFSubmissionVerdict.PARTIAL)
                val score = submission.points?.takeIf { !isWrong } ?: 0.0
                IOIRunResult(
                    score = listOf(score),
                    wrongVerdict = submission.processedVerdict.takeIf { isWrong },
                )
            }
            CFContestType.CF -> {
                // TODO: this should come from API
                val maxScore = submission.problem.points!!
                val isWrong = submission.verdict !in listOf(CFSubmissionVerdict.OK, CFSubmissionVerdict.CHALLENGED, CFSubmissionVerdict.SKIPPED)
                val score = if (!isWrong) {
                    maxOf(
                        maxScore * 3 / 10,
                        ceil(maxScore - submission.relativeTimeSeconds.inWholeMinutes * getProblemLooseScorePerMinute(maxScore, contestLength.inWholeMinutes) - 50 * wrongAttempts)
                    )
                } else {
                    0.0
                }
                IOIRunResult(
                    score = listOf(score),
                    wrongVerdict = submission.processedVerdict.takeIf { isWrong },
                )
            }
        }
    }

    private fun getProblemLooseScorePerMinute(initialScore: Double, duration: Long): Double {
        val finalScore = initialScore * 0.52
        val roundedContestDuration = maxOf(1, duration / 30) * 30
        return (initialScore - finalScore) / roundedContestDuration
    }

    fun parseSubmissions(submissions: List<CFSubmission>): List<RunInfo> {
        val problemTestsCount = submissions.groupBy { it.problem.index }.mapValues { (_, v) ->
            v.maxOf { submit -> submit.passedTestCount + if (submit.verdict == CFSubmissionVerdict.OK) 0 else 1 }
        }
        return submissions.reversed().asSequence()
            .filter { it.author.participantType == CFPartyParticipantType.CONTESTANT }
            .filter { getTeamCdsId(it.author) in participantsByCdsId }
            .groupBy { it.author to it.problem }
            .mapValues {(_, submissions) ->
                var wrongs = 0
                submissions.sortedBy { it.id }.map {
                    val problemId = problemsMap[it.problem.index]!!.id
                    val problemTests = problemTestsCount[it.problem.index]!!
                    val result = submissionToResult(it, wrongs)
                    val run = RunInfo(
                        id = it.id.toInt(),
                        result = result,
                        problemId = problemId,
                        teamId = participantsByCdsId[getTeamCdsId(it.author)]!!.id,
                        percentage = if (result != null) 1.0 else (it.passedTestCount.toDouble() / problemTests),
                        time = it.relativeTimeSeconds,
                    )
                    wrongs += if (it.processedVerdict?.isAddingPenalty == true) 1 else 0
                    run
                }
            }.values.flatten()
    }

    fun parseHacks(hacks: List<CFHack>) : List<RunInfo> {
        return buildList {
            for (hack in hacks) {
                if (hack.creationTimeSeconds - startTime > contestLength) continue
                if (hack.hacker.participantType == CFPartyParticipantType.CONTESTANT) {
                    add(
                        RunInfo(
                            id = (hack.id * 2).inv(),
                            result = when (hack.verdict) {
                                CFHackVerdict.HACK_SUCCESSFUL -> IOIRunResult(
                                    score = listOf(100.0),
                                )

                                CFHackVerdict.HACK_UNSUCCESSFUL -> IOIRunResult(
                                    score = listOf(-50.0),
                                )

                                CFHackVerdict.TESTING -> null
                                else -> IOIRunResult(
                                    score = emptyList(),
                                    wrongVerdict = Verdict.CompilationError,
                                )
                            },
                            percentage = 0.0,
                            problemId = -1,
                            teamId = participantsByCdsId[getTeamCdsId(hack.hacker)]!!.id,
                            time = hack.creationTimeSeconds - startTime
                        )
                    )
                }
                if (hack.defender.participantType == CFPartyParticipantType.CONTESTANT) {
                    add(
                        RunInfo(
                            id = (hack.id * 2 + 1).inv(),
                            result = IOIRunResult(
                                score = listOf(0.0),
                                wrongVerdict = if (hack.verdict == CFHackVerdict.HACK_SUCCESSFUL) null else Verdict.Accepted,
                            ),
                            isHidden = hack.verdict != CFHackVerdict.HACK_SUCCESSFUL,
                            percentage = 0.0,
                            problemId = problemsMap[hack.problem.index]!!.id,
                            teamId = participantsByCdsId[getTeamCdsId(hack.defender)]!!.id,
                            time = hack.creationTimeSeconds - startTime
                        )
                    )
                }
            }
        }
    }

    fun toApi() = ContestInfo(
        name = name,
        status = status,
        resultType = when (contestType) {
            CFContestType.CF -> ContestResultType.IOI
            CFContestType.IOI -> ContestResultType.IOI
            CFContestType.ICPC -> ContestResultType.ICPC
        },
        startTime = startTime,
        contestLength = contestLength,
        freezeTime = contestLength,
        problems = problems,
        teams = participantsByCdsId.values.sortedBy { it.id },
        groups = emptyList(),
        penaltyRoundingMode = when (contestType) {
            CFContestType.CF -> PenaltyRoundingMode.ZERO
            CFContestType.IOI -> PenaltyRoundingMode.ZERO
            CFContestType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
        },
        organizations = emptyList()
    )

    companion object {
        fun getTeamCdsId(party: CFParty): String {
            return party.teamId?.let { "team:${it}" } ?: party.members[0].handle
        }
    }
}
