package org.icpclive.cds.plugins.krsu

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.DataLoader
import org.icpclive.cds.ktor.KtorNetworkSettingsProvider
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.ksp.cds.Builder
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@Builder("krsu")
public sealed interface KRSUSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val submissionsSource: UrlOrLocalPath
    public val contestSource: UrlOrLocalPath
    public val timeZone: TimeZone
        get() = TimeZone.of("Asia/Bishkek")
    public val resultType: ContestResultType
        get() = ContestResultType.ICPC
    override fun toDataSource(): ContestDataSource = KRSUDataSource(this)
}

internal class KRSUDataSource(val settings: KRSUSettings) : FullReloadContestDataSource(5.seconds) {
    override suspend fun loadOnce() = parseAndUpdateStandings(
        contestInfoLoader.load(), submissionsLoader.load()
    )

    private val resultType = settings.resultType
    private val teams = mutableMapOf<String, TeamInfo>()

    private fun parseAndUpdateStandings(contest: Contest, submissions: List<Submission>): ContestParseResult {
        val startTime = contest.StartTime.toInstant(settings.timeZone)

        val problemsList = contest.ProblemSet.mapIndexed { index, it ->
            ProblemInfo(
                id = it.Problem.toProblemId(),
                displayName = "" + ('A' + index),
                fullName = "" + ('A' + index),
                ordinal = index,
                minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                maxScore = if (resultType == ContestResultType.IOI) 100.0 else null,
                scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.MAX_TOTAL else null
            )
        }
//        val problemById = problemsList.associateBy { it.id }

        for (submission in submissions) {
            if (!teams.contains(submission.Login)) {
                teams[submission.Login] =
                    TeamInfo(
                        id = submission.Login.toTeamId(),
                        fullName = submission.AuthorName,
                        displayName = submission.AuthorName,
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
                id = it.Id.toRunId(),
                result = when (resultType) {
                    ContestResultType.IOI ->
                        RunResult.IOI(
                            score = listOf(it.Points)
                        )
                    ContestResultType.ICPC ->
                        result?.toICPCRunResult() ?: RunResult.InProgress(0.0)
                },
                problemId = it.Problem.toProblemId(),
                teamId = it.Login.toTeamId(),
                time = (it.ReceivedTime.toInstant(settings.timeZone)) - startTime,
                languageId = null,
            )
        }.toList()

        return ContestParseResult(
            ContestInfo(
                name = "",
                resultType = resultType,
                startTime = startTime,
                contestLength = contestLength,
                freezeTime = freezeTime,
                problemList = problemsList,
                teamList = teams.values.toList(),
                groupList = emptyList(),
                organizationList = emptyList(),
                penaltyRoundingMode = when (resultType) {
                    ContestResultType.IOI -> PenaltyRoundingMode.ZERO
                    ContestResultType.ICPC -> PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE
                },
                languagesList = emptyList()
            ),
            runs,
            emptyList()
        )
    }

    private val submissionsLoader = DataLoader.json<List<Submission>>(networkSettings = settings.network) { settings.submissionsSource }
    private val contestInfoLoader = DataLoader.json<Contest>(networkSettings = settings.network) { settings.contestSource }

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
            "Partial Solution" to Verdict.Accepted,
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
        val Points: Double,
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

