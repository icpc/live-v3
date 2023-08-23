package org.icpclive.cds.krsu

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.KRSUSettings
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

internal class KRSUDataSource(val settings: KRSUSettings) : FullReloadContestDataSource(5.seconds) {
    override suspend fun loadOnce() = parseAndUpdateStandings(
        contestInfoLoader.load(), submissionsLoader.load()
    )

    val teams = mutableMapOf<String, TeamInfo>()
    var lastTeamId: Int = 0

    private fun parseAndUpdateStandings(contest: Contest, submissions: List<Submission>): ContestParseResult {
        val startTime = contest.StartTime.toInstant(settings.timeZone)

        val problemsList = contest.ProblemSet.mapIndexed { index, it ->
            ProblemInfo(
                letter = "" + ('A' + index),
                name = "" + ('A' + index),
                id = it.Problem,
                ordinal = index,
                contestSystemId = index.toString(),
            )
        }
//        val problemById = problemsList.associateBy { it.id }

        for (submission in submissions) {
            if (!teams.contains(submission.Login)) {
                teams[submission.Login] =
                    TeamInfo(
                        id = lastTeamId++,
                        fullName = submission.AuthorName,
                        displayName = submission.AuthorName,
                        contestSystemId = submission.Login,
                        groups = emptyList(),
                        hashTag = null,
                        medias = emptyMap(),
                        isOutOfContest = false,
                        isHidden = false,
                        organizationId = null
                    )
            }
        }
        val contestLength = contest.Length.hours
        val freezeTime = contestLength - 1.hours
        val runs = submissions.map {
            val result = outcomeMap[it.StatusName]
            RunInfo(
                id = it.Id,
                result?.toRunResult(),
                problemId = it.Problem,
                teamId = teams[it.Login]?.id ?: -1,
                percentage = if (result == null) 0.0 else 1.0,
                time = (it.ReceivedTime.toInstant(settings.timeZone)) - startTime,
            )
        }.toList()

        return ContestParseResult(
            ContestInfo(
                name = "",
                status = ContestStatus.byCurrentTime(startTime, contestLength),
                resultType = ContestResultType.ICPC,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = problemsList,
                teamList = teams.values.toList(),
                groupList = emptyList(),
                organizationList = emptyList(),
                penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
            ),
            runs,
            emptyList()
        )
    }

    private val submissionsLoader = jsonLoader<List<Submission>>(networkSettings = settings.network) { settings.submissionsUrl }
    private val contestInfoLoader = jsonLoader<Contest>(networkSettings = settings.network) { settings.contestUrl }

    companion object {
        private val outcomeMap = mapOf(
            "InternalError" to Verdict.Fail,
            "Compile Error" to Verdict.CompilationError,
            "Run-Time Error" to Verdict.RuntimeError,
            "Time Limit Exceeded" to Verdict.TimeLimitExceeded,
            "Memory Limit Exceeded" to Verdict.MemoryLimitExceeded,
            "Output Limit Exceeded" to Verdict.OutputLimitExceeded,
            "Security Violation" to Verdict.SecurityViolation,
            "Wrong Answer" to Verdict.WrongAnswer,
            "Accepted" to Verdict.Accepted,
            "Presentation Error" to Verdict.PresentationError,
        )
    }

    @Serializable
    @Suppress("unused")
    class Submission(
        val Id: Int,
        val Login: String,
        val Problem: Int,
        val Letter: Int,
        val Target: String,
        val Status: Int,
        val StatusName: String,
        val TestPassed: Int,
        val ReceivedTime: LocalDateTime,
        val AuthorName: String,
    )

    @Serializable
    @Suppress("unused")
    class Contest(
        val Id: Int,
        val ProblemSet: List<Problem>,
        val StartTime: LocalDateTime,
        val Length: Int
    )

    @Serializable
    @Suppress("unused")
    class Problem(
        val Letter: Int,
        val Problem: Int
    )
}

