package org.icpclive.cds.plugins.allcups

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.common.FullReloadContestDataSource
import org.icpclive.cds.ksp.GenerateSettings
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.Credential
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@GenerateSettings("allcups")
public interface AllCupsSettings : CDSSettings {
    public val contestId: Int
    public val token: Credential
    public val contestName: String
    public val startTime: Instant
    public val contestLength: Duration
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
    private val loader = jsonUrlLoader<List<Submission>>(settings.network, ClientAuth.Bearer(settings.token.value)) {
        "https://cups.online/api_live/submissions/round/${settings.contestId}/"
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
        id = id,
        result = code.toVerdict().toRunResult(),
        percentage = 1.0,
        problemId = task_id,
        teamId = user_id,
        time = elapsed_seconds.seconds
    )

    override suspend fun loadOnce(): ContestParseResult {
        val submissions = loader.load()
        return ContestParseResult(
            ContestInfo(
                name = settings.contestName,
                status = ContestStatus.byCurrentTime(settings.startTime, settings.contestLength),
                resultType = ContestResultType.ICPC,
                startTime = settings.startTime,
                contestLength = settings.contestLength,
                freezeTime = settings.freezeTime,
                penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
                groupList = emptyList(),
                organizationList = emptyList(),
                teamList = settings.teamIds.map {
                    TeamInfo(
                        id = it,
                        displayName = "",
                        fullName = "",
                        contestSystemId = it.toString(),
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
                        id = id,
                        contestSystemId = id.toString(),
                        displayName = "",
                        fullName = "",
                        ordinal = index
                    )
                }
            ),
            submissions.filter { it.elapsed_seconds <= settings.contestLength.inWholeSeconds }.map { it.toRun() },
            emptyList()
        )
    }

}