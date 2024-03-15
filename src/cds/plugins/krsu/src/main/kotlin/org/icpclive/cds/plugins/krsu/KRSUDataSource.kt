package org.icpclive.cds.plugins.krsu

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ksp.Builder
import org.icpclive.cds.ktor.jsonUrlLoader
import org.icpclive.cds.settings.CDSSettings
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Builder("krsu")
public sealed interface KRSUSettings : CDSSettings {
    public val submissionsUrl: String
    public val contestUrl: String
    public val timeZone: TimeZone
        get() = TimeZone.of("Asia/Bishkek")

    override fun toDataSource(): ContestDataSource = KRSUDataSource(this)
}

internal class KRSUDataSource(val settings: KRSUSettings) : FullReloadContestDataSource(5.seconds) {
    override suspend fun loadOnce() = parseAndUpdateStandings(
        contestInfoLoader.load(), submissionsLoader.load()
    )

    private val teams = mutableMapOf<String, TeamInfo>()
    private var lastTeamId: Int = 0

    private fun parseAndUpdateStandings(contest: Contest, submissions: List<Submission>): ContestParseResult {
        val startTime = contest.StartTime.toInstant(settings.timeZone)

        val problemsList = contest.ProblemSet.mapIndexed { index, it ->
            ProblemInfo(
                displayName = "" + ('A' + index),
                fullName = "" + ('A' + index),
                ordinal = index,
                contestSystemId = it.Problem.toString(),
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
                result?.toICPCRunResult() ?: RunResult.InProgress(0.0),
                problemId = it.Problem.toString(),
                teamId = teams[it.Login]?.id ?: -1,
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

    private val submissionsLoader = jsonUrlLoader<List<Submission>>(networkSettings = settings.network) { settings.submissionsUrl }
    private val contestInfoLoader = jsonUrlLoader<Contest>(networkSettings = settings.network) { settings.contestUrl }

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
    class Contest(
        val Id: Int,
        val ProblemSet: List<Problem>,
        val StartTime: LocalDateTime,
        val Length: Int,
    )

    @Serializable
    @Suppress("unused")
    class Problem(
        val Letter: Int,
        val Problem: Int,
    )
}

