package org.icpclive.cds.codeforces

import org.icpclive.cds.EventsLoader
import org.icpclive.cds.RunInfo
import org.icpclive.cds.codeforces.api.data.CFSubmission
import org.icpclive.cds.codeforces.api.data.CFSubmission.CFSubmissionVerdict

/**
 * @author egor@egork.net
 */
class CFRunInfo(var submission: CFSubmission) : RunInfo {
    private var lastUpdate: Long
    var points = 0

    init {
        lastUpdate = submission.relativeTimeSeconds
    }

    override val id: Int
        get() = submission.id.toInt()
    override val isAccepted: Boolean
        get() = submission.verdict == CFSubmissionVerdict.OK
    override val isAddingPenalty: Boolean
        get() = false
    override val isJudged: Boolean
        get() = submission.verdict != CFSubmissionVerdict.TESTING
    override val result: String
        get() = if (submission.verdict == null) {
            ""
        } else verdictToString[submission.verdict]!!
    val problem: CFProblemInfo
        get() = CFEventsLoader.instance.contestData.getProblem(submission.problem)!!
    override val problemId: Int
        get() = problem.id
    override val teamId: Int
        get() {
            val participant = CFEventsLoader.instance.contestData.getParticipant(
                CFContestInfo.getName(
                    submission.author
                )
            )
            return participant!!.id
        }
    override val percentage: Double
        get() = submission.passedTestCount.toDouble() / problem.totalTests
    override val time: Long
        get() = submission.relativeTimeSeconds * 1000
    override val lastUpdateTime: Long
        get() = lastUpdate * 1000

    fun updateFrom(submission: CFSubmission, contestTime: Long) {
        if (submission.verdict != this.submission.verdict || submission.passedTestCount != this.submission.passedTestCount) {
            this.submission = submission
            lastUpdate = contestTime
        }
    }

    override val isFirstSolvedRun: Boolean
        get() = CFEventsLoader.instance.contestData.firstSolvedRun[problemId] === this


    companion object {
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
    }
}