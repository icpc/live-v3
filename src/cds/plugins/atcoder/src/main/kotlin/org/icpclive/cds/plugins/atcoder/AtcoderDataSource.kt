package org.icpclive.cds.plugins.atcoder

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.ksp.cds.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
internal class AtcoderTask(
    val Assignment: String,
    val TaskName: String,
    val TaskScreenName: String,
)

@Serializable
internal class AtcoderTaskResult(
    val Count: Int,
    val Failure: Int,
    val Penalty: Int,
    val Score: Int,
    val Elapsed: Long,
    val Pending: Boolean,
)

@Serializable
internal class AtcoderTeam(
    val UserScreenName: String,
    val TaskResults: Map<String, AtcoderTaskResult>,
)

@Serializable
internal class ContestData(
    val TaskInfo: List<AtcoderTask>,
    val StandingsData: List<AtcoderTeam>,
)

@Builder("atcoder")
public sealed interface AtcoderSettings : CDSSettings {
    public val contestId: String
    public val sessionCookie: Credential
    @Human
    public val startTime: Instant
    @Seconds
    public val contestLength: Duration
    override fun toDataSource(): ContestDataSource = AtcoderDataSource(this)
}


internal class AtcoderDataSource(val settings: AtcoderSettings) : FullReloadContestDataSource(5.seconds) {
    private val loader = DataLoader.json<ContestData>(
        settings.network,
        ClientAuth.cookie("REVEL_SESSION", settings.sessionCookie.value),
        UrlOrLocalPath.Url("https://atcoder.jp/contests/").subDir(settings.contestId).subDir("standings").subDir("json")
    )

    private var submissionId: Int = 1
    val runs = mutableMapOf<Pair<TeamId, ProblemId>, List<RunInfo>>()

    private fun addNewRuns(teamId: TeamId, problemId: ProblemId, result: AtcoderTaskResult): List<RunInfo> {
        val oldRuns = (runs[teamId to problemId] ?: emptyList()).toMutableList()
        repeat(result.Count - oldRuns.size) {
            oldRuns.add(
                RunInfo(
                    id = submissionId++.toRunId(),
                    result = RunResult.InProgress(0.0),
                    problemId = problemId,
                    teamId = teamId,
                    time = minOf(settings.contestLength, Clock.System.now() - settings.startTime)
                )
            )
        }
        while (oldRuns.count { (it.result as? RunResult.IOI)?.wrongVerdict != null } < result.Penalty) {
            val fst = oldRuns.indexOfFirst { it.result is RunResult.InProgress }
            oldRuns[fst] = oldRuns[fst].copy(result = RunResult.IOI(score = listOf(0.0), wrongVerdict = Verdict.Rejected))
            if (result.Elapsed.nanoseconds != ZERO && oldRuns[fst].time > result.Elapsed.nanoseconds) {
                oldRuns[fst] = oldRuns[fst].copy(time = result.Elapsed.nanoseconds)
            }
        }
        if (result.Score > 0) {
            if (oldRuns.mapNotNull { it.result as? RunResult.IOI }.maxOfOrNull { it.score[0] }?.toInt() != result.Score / 100 && !result.Pending) {
                val fst = oldRuns.indexOfFirst { it.result is RunResult.InProgress }
                oldRuns[fst] = oldRuns[fst].copy(
                    result = RunResult.IOI(score = listOf(result.Score / 100.0)),
                    time = result.Elapsed.nanoseconds
                )
            }
        }
        return oldRuns
    }

    override suspend fun loadOnce(): ContestParseResult {
        val data = loader.load()
        val problems = data.TaskInfo.mapIndexed { index, task ->
            ProblemInfo(
                id = task.TaskScreenName.toProblemId(),
                displayName = task.Assignment,
                fullName = task.TaskName,
                ordinal = index,
                minScore = 0.0,
                maxScore = (data.StandingsData.maxOfOrNull { it.TaskResults[task.TaskScreenName]?.Score ?: 0 } ?: 0) / 100.0,
                scoreMergeMode = ScoreMergeMode.LAST_OK
            )
        }
        val teams = data.StandingsData.map {
            TeamInfo(
                id = it.UserScreenName.toTeamId(),
                fullName = it.UserScreenName,
                displayName = it.UserScreenName,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isHidden = false,
                isOutOfContest = false,
                organizationId = null,
            )
        }.sortedBy { it.id.value }

        val info = ContestInfo(
            name = "",
            status = ContestStatus.byCurrentTime(settings.startTime, settings.contestLength),
            resultType = ContestResultType.IOI,
            startTime = settings.startTime,
            contestLength = settings.contestLength,
            freezeTime = settings.contestLength,
            problemList = problems,
            teamList = teams,
            groupList = emptyList(),
            organizationList = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.LAST,
            penaltyPerWrongAttempt = 5.minutes,
        )
        val newRuns = buildList {
            for (teamResult in data.StandingsData) {
                val teamId = teamResult.UserScreenName.toTeamId()
                for ((problemId, problemResult) in teamResult.TaskResults) {
                    runs[teamId to problemId.toProblemId()] = addNewRuns(teamId, problemId.toProblemId(), problemResult).also {
                        addAll(it)
                    }
                }
            }
        }
        return ContestParseResult(info, newRuns, emptyList())
    }
}