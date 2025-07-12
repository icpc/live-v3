package org.icpclive.cds.plugins.codeforces

import org.icpclive.cds.api.*
import org.icpclive.cds.plugins.codeforces.api.data.*
import org.icpclive.cds.plugins.codeforces.api.results.CFStandings
import kotlin.math.ceil
import kotlin.time.*
import kotlin.time.Duration.Companion.hours


private val HACKS_PROBLEM_ID = "hacks".toProblemId()

internal class CFContestInfo {
    private var contestLength: Duration = 5.hours
    private var freezeDuration: Duration? = null
    private var startTime: Instant = Instant.fromEpochMilliseconds(0)
    var status: ContestStatus = ContestStatus.BEFORE()
        private set
    private val problems = mutableListOf<ProblemInfo>()
    private var cfStandings: CFStandings? = null
    private val participants = mutableListOf<TeamInfo>()
    private var contestType: CFContestType = CFContestType.ICPC
    private var name: String = ""

    private fun updateContestInfo(contest: CFContest) {
        name = contest.name
        contestType = contest.type
        contestLength = contest.duration!!
        startTime = contest.startTime
            ?: Instant.DISTANT_FUTURE
        status = when (contest.phase) {
            CFContestPhase.BEFORE -> ContestStatus.BEFORE(scheduledStartAt = startTime.takeUnless{ it.isDistantFuture })
            CFContestPhase.CODING -> ContestStatus.RUNNING(startedAt = startTime, frozenAt = null)
            else -> ContestStatus.OVER(startedAt = startTime, finishedAt = startTime + contestLength, frozenAt = null)
        }
        freezeDuration = contest.freezeDuration
    }

    fun updateStandings(standings: CFStandings) {
        this.cfStandings = standings
        updateContestInfo(standings.contest)
        problems.clear()
        for ((id, problem) in standings.problems.withIndex()) {
            val problemInfo = ProblemInfo(
                id = problem.index.toProblemId(),
                displayName = if (contestType == CFContestType.BLITZ) problem.points!!.toInt().toString() else problem.index,
                fullName = problem.name!!,
                ordinal = id,
                minScore = if (problem.points != null) 0.0 else null,
                maxScore = problem.points,
                scoreMergeMode = when (contestType) {
                    CFContestType.CF -> ScoreMergeMode.LAST_OK
                    CFContestType.ICPC, CFContestType.BLITZ -> null
                    CFContestType.IOI -> ScoreMergeMode.MAX_TOTAL
                },
                weight = if (contestType == CFContestType.BLITZ) problem.points!!.toInt() else 1
            )
            problems.add(problemInfo)
        }
        if (contestType == CFContestType.CF) {
            val hacksInfo = ProblemInfo(
                id = HACKS_PROBLEM_ID,
                displayName = "*",
                fullName = "Hacks",
                ordinal = -1,
                minScore = null,
                maxScore = null,
                scoreMergeMode = ScoreMergeMode.SUM,
                ftsMode = FtsMode.Hidden
            )
            problems.add(hacksInfo)
        }
        participants.clear()
        for (row in standings.rows) {
            val cdsId = getTeamCdsId(row.party)
            val party = row.party
            participants.add(TeamInfo(
                id = cdsId.toTeamId(),
                fullName = party.teamName ?: party.members[0].let { it.name ?: it.handle },
                displayName = party.teamName ?: party.members[0].let { it.name ?: it.handle },
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                organizationId = null,
                isHidden = false,
                isOutOfContest = false
            ))
        }
    }

    private val CFSubmission.processedVerdict
        get() = when (verdict) {
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

    private fun submissionToResult(submission: CFSubmission, wrongAttempts: Int): RunResult? {
        if (submission.verdict == null || submission.verdict == CFSubmissionVerdict.TESTING) return null
        return when (contestType) {
            CFContestType.ICPC -> {
                submission.processedVerdict?.toICPCRunResult()
            }

            CFContestType.BLITZ -> {
                val prelim = submission.processedVerdict?.toICPCRunResult()
                if (prelim?.verdict == Verdict.Accepted && submission.points == null) {
                    Verdict.Ignored.toICPCRunResult()
                } else {
                    prelim
                }
            }

            CFContestType.IOI -> {
                // TODO: is CFSubmissionVerdict.SKIPPED not wrong
                val isWrong = submission.verdict !in listOf(
                    CFSubmissionVerdict.OK,
                    CFSubmissionVerdict.CHALLENGED,
                    CFSubmissionVerdict.PARTIAL
                )
                val score = submission.points?.takeIf { !isWrong } ?: 0.0
                RunResult.IOI(
                    score = listOf(score),
                    wrongVerdict = submission.processedVerdict.takeIf { isWrong },
                )
            }

            CFContestType.CF -> {
                // TODO: this should come from API
                val maxScore = submission.problem.points!!
                val isWrong = submission.verdict !in listOf(
                    CFSubmissionVerdict.OK,
                    CFSubmissionVerdict.CHALLENGED,
                    CFSubmissionVerdict.SKIPPED
                )
                val score = if (!isWrong) {
                    maxOf(
                        maxScore * 3 / 10,
                        ceil(
                            maxScore - submission.relativeTimeSeconds.inWholeMinutes * getProblemLooseScorePerMinute(
                                maxScore,
                                contestLength.inWholeMinutes
                            ) - 50 * wrongAttempts
                        )
                    )
                } else {
                    0.0
                }
                RunResult.IOI(
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
            .groupBy { it.author to it.problem }
            .mapValues { (_, submissions) ->
                var wrongs = 0
                submissions.sortedBy { it.id }.map {
                    val problemId = it.problem.index.toProblemId()
                    val problemTests = problemTestsCount[it.problem.index]
                    val result = submissionToResult(it, wrongs)
                    val run = RunInfo(
                        id = it.id.toRunId(),
                        result = result ?: RunResult.InProgress(if (problemTests == null) 0.0 else it.passedTestCount.toDouble() / problemTests),
                        problemId = problemId,
                        teamId = getTeamCdsId(it.author).toTeamId(),
                        time = it.relativeTimeSeconds,
                        languageId = it.programmingLanguage.toLanguageId()
                    )
                    wrongs += if (it.processedVerdict?.isAddingPenalty == true) 1 else 0
                    run
                }
            }.values.flatten()
    }

    fun parseHacks(hacks: List<CFHack>): List<RunInfo> {
        return buildList {
            for (hack in hacks) {
                if (hack.creationTimeSeconds - startTime > contestLength) continue
                if (hack.hacker.participantType == CFPartyParticipantType.CONTESTANT) {
                    add(
                        RunInfo(
                            id = "hack-attack-${hack.id}".toRunId(),
                            result = when (hack.verdict) {
                                CFHackVerdict.HACK_SUCCESSFUL -> RunResult.IOI(
                                    score = listOf(100.0),
                                )

                                CFHackVerdict.HACK_UNSUCCESSFUL -> RunResult.IOI(
                                    score = listOf(-50.0),
                                )

                                CFHackVerdict.TESTING -> RunResult.InProgress(0.0)
                                else -> RunResult.IOI(
                                    score = emptyList(),
                                    wrongVerdict = Verdict.CompilationError,
                                )
                            },
                            problemId = HACKS_PROBLEM_ID,
                            teamId = getTeamCdsId(hack.hacker).toTeamId(),
                            time = hack.creationTimeSeconds - startTime,
                            languageId = null
                        )
                    )
                }
                if (hack.defender.participantType == CFPartyParticipantType.CONTESTANT) {
                    add(
                        RunInfo(
                            id = "hack-defend-${hack.id}".toRunId(),
                            result = RunResult.IOI(
                                score = listOf(0.0),
                                wrongVerdict = if (hack.verdict == CFHackVerdict.HACK_SUCCESSFUL) null else Verdict.Accepted,
                            ),
                            isHidden = hack.verdict != CFHackVerdict.HACK_SUCCESSFUL,
                            problemId = hack.problem.index.toProblemId(),
                            teamId = getTeamCdsId(hack.defender).toTeamId(),
                            time = hack.creationTimeSeconds - startTime,
                            languageId = null
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
            CFContestType.CF, CFContestType.IOI -> ContestResultType.IOI
            CFContestType.ICPC, CFContestType.BLITZ -> ContestResultType.ICPC
        },
        contestLength = contestLength,
        freezeTime = freezeDuration?.let { contestLength - it },
        problemList = problems.sortedBy { it.ordinal },
        teamList = participants.sortedBy { it.id.value },
        groupList = emptyList(),
        penaltyRoundingMode = when (contestType) {
            CFContestType.CF -> PenaltyRoundingMode.ZERO
            CFContestType.IOI -> PenaltyRoundingMode.ZERO
            CFContestType.BLITZ -> PenaltyRoundingMode.ZERO
            CFContestType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
        },
        organizationList = emptyList(),
        languagesList = emptyList(),
        awardsSettings = AwardsSettings(
            firstToSolveProblems = contestType != CFContestType.BLITZ,
        )
    )

    companion object {
        fun getTeamCdsId(party: CFParty): String {
            return party.teamId?.let { "team:${it}" } ?: party.members[0].handle
        }
    }
}
