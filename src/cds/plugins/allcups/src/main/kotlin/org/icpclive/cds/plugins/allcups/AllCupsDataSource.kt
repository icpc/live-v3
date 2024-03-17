package org.icpclive.cds.plugins.allcups

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.ksp.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.Credential
import org.icpclive.util.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Builder("allcups")
public sealed interface AllCupsSettings : CDSSettings {
    public val contestId: Int
    public val token: Credential
    public val contestName: String
    @Human public val startTime: Instant
    @Seconds public val contestLength: Duration
    @Seconds public val freezeTime: Duration
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
        id = RunId(id.toString()),
        result = code.toVerdict().toICPCRunResult(),
        problemId = ProblemId(task_id.toString()),
        teamId = TeamId(user_id.toString()),
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
                        id = TeamId(it.toString()),
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
                        id = ProblemId(id.toString()),
                        displayName = "",
                        fullName = "",
                        ordinal = index
                    )
                }
            ),
            submissions
                .filter { it.elapsed_seconds <= settings.contestLength.inWholeSeconds }
                .filter {
                    when  {
                        it.user_id !in settings.teamIds -> {
                            logger.error("Submission from unknown user ${it.user_id}")
                            false
                        }
                        it.task_id !in settings.problemIds -> {
                            logger.error("Submission for unknown problem ${it.task_id}")
                            false
                        }
                        else -> true
                    }
                }
                .map { it.toRun() },
            emptyList()
        )
    }

    companion object {
        val logger = getLogger(AllCupsDataSource::class)
    }
}