package org.icpclive.cds.plugins.allcups

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.getLogger
import org.icpclive.ksp.cds.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Builder("allcups")
public sealed interface AllCupsSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val contestId: Int
    public val token: Credential
    public val contestName: String
    @Human
    public val startTime: Instant
    @Seconds
    public val contestLength: Duration
    @Seconds
    public val freezeTime: Duration
    public val problemIds: List<Int>
    public val teamIds: List<Int>
    override fun toDataSource(): ContestDataSource = AllCupsDataSource(this)
}

@Serializable
internal class Submission(
    val id: Int,
    val user_id: Int,
    val task_id: Int,
    val elapsed_seconds: Double,
    val code: String
)

internal class AllCupsDataSource(val settings: AllCupsSettings) : FullReloadContestDataSource(5.seconds) {
    private val urlRoot = UrlOrLocalPath.Url("https://cups.online/api_live/submissions/round")
        .withBearer(settings.token)
    private val loader = DataLoader.json<List<Submission>>(settings.network) {
        urlRoot.subDir(settings.contestId.toString())
    }

    private fun String.toVerdict() = when (this) {
        "OK" -> Verdict.Accepted
        "WA" -> Verdict.WrongAnswer
        "TL" -> Verdict.TimeLimitExceeded
        "CE" -> Verdict.CompilationError
        "PE" -> Verdict.PresentationError
        "RE" -> Verdict.RuntimeError
        "ML" -> Verdict.MemoryLimitExceeded
        "SE" -> Verdict.CompilationError
        "CR" -> Verdict.RuntimeError
        else -> error("Unknown verdict ${this}")
    }

    private fun Submission.toRun() = RunInfo(
        id = id.toRunId(),
        result = code.toVerdict().toICPCRunResult(),
        problemId = task_id.toProblemId(),
        teamId = user_id.toTeamId(),
        time = elapsed_seconds.seconds,
        languageId = null
    )

    override suspend fun loadOnce(): ContestParseResult {
        val submissions = loader.load()
        val runs = submissions
            .filter { it.elapsed_seconds <= settings.contestLength.inWholeSeconds }
            .filter {
                when {
                    it.user_id !in settings.teamIds -> {
                        log.error { "Submission from unknown user ${it.user_id}" }
                        false
                    }

                    it.task_id !in settings.problemIds -> {
                        log.error { "Submission for unknown problem ${it.task_id}" }
                        false
                    }

                    else -> true
                }
            }
            .map { it.toRun() }
        return ContestParseResult(
            ContestInfo(
                name = settings.contestName,
                resultType = ContestResultType.ICPC,
                startTime = settings.startTime,
                contestLength = settings.contestLength,
                freezeTime = settings.freezeTime,
                penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
                groupList = emptyList(),
                organizationList = emptyList(),
                teamList = settings.teamIds.map {
                    TeamInfo(
                        id = it.toTeamId(),
                        displayName = "",
                        fullName = "",
                        groups = emptyList(),
                        hashTag = null,
                        medias = emptyMap(),
                        isHidden = false,
                        isOutOfContest = false,
                        organizationId = null
                    )
                },
                problemList = settings.problemIds.mapIndexed { index, id ->
                    ProblemInfo(
                        id = id.toProblemId(),
                        displayName = "",
                        fullName = "",
                        ordinal = index
                    )
                },
                languagesList = runs.languages(),
            ),
            runs,
            emptyList()
        )
    }

    companion object {
        val log by getLogger()
    }
}