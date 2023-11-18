package org.icpclive.cds.atcoder

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.cds.common.*
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.common.FullReloadContestDataSource
import org.icpclive.cds.common.jsonLoader
import org.icpclive.cds.settings.AtcoderSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.util.*
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
    val TaskResults: Map<String, AtcoderTaskResult>
)

@Serializable
internal class ContestData(
    val TaskInfo: List<AtcoderTask>,
    val StandingsData: List<AtcoderTeam>
)

internal class AtcoderDataSource(val settings: AtcoderSettings) : FullReloadContestDataSource(5.seconds) {
    val teamIds = Enumerator<String>()
    val problemIds = Enumerator<String>()
    private val loader = jsonLoader<ContestData>(settings.network, ClientAuth.CookieAuth("REVEL_SESSION", settings.sessionCookie.value)) {
        UrlOrLocalPath.Url("https://atcoder.jp/contests/${settings.contestId}/standings/json")
    }

    var submissionId: Int = 1
    val runs = mutableMapOf<Pair<Int, Int>, List<RunInfo>>()

    fun addNewRuns(teamId: Int, problemId: Int, result: AtcoderTaskResult) : List<RunInfo> {
        val oldRuns = (runs[teamId to problemId] ?: emptyList()).toMutableList()
        repeat(result.Count - oldRuns.size) {
            oldRuns.add(
                RunInfo(
                    id = submissionId++,
                    result = null,
                    percentage = 0.0,
                    problemId = problemId,
                    teamId = teamId,
                    time = minOf(settings.contestLength, Clock.System.now() - settings.startTime)
                )
            )
        }
        while (oldRuns.count { (it.result as? IOIRunResult)?.wrongVerdict != null } < result.Penalty) {
            val fst = oldRuns.indexOfFirst { it.result == null }
            oldRuns[fst] = oldRuns[fst].copy(result = IOIRunResult(score = listOf(0.0), wrongVerdict = Verdict.Rejected))
            if (result.Elapsed.nanoseconds != ZERO && oldRuns[fst].time > result.Elapsed.nanoseconds) {
                oldRuns[fst] = oldRuns[fst].copy(time = result.Elapsed.nanoseconds)
            }
        }
        if (result.Score > 0) {
            if (oldRuns.mapNotNull { it.result as? IOIRunResult }.maxOfOrNull { it.score[0] }?.toInt() != result.Score / 100 && !result.Pending) {
                val fst = oldRuns.indexOfFirst { it.result == null }
                oldRuns[fst] = oldRuns[fst].copy(result = IOIRunResult(score = listOf(result.Score / 100.0)), time = result.Elapsed.nanoseconds)
            }
        }
        return oldRuns
    }

    override suspend fun loadOnce(): ContestParseResult {
        val data = loader.load()
        val problems = data.TaskInfo.mapIndexed { index, task ->
            ProblemInfo(
                displayName = task.Assignment,
                fullName = task.TaskName,
                id = problemIds[task.TaskScreenName],
                ordinal = index,
                contestSystemId = task.TaskScreenName,
                minScore = 0.0,
                maxScore = (data.StandingsData.maxOfOrNull { it.TaskResults[task.TaskScreenName]?.Score ?: 0 } ?: 0) / 100.0,
                scoreMergeMode = ScoreMergeMode.LAST_OK
            )
        }
        val teams = data.StandingsData.map {
            TeamInfo(
                id = teamIds[it.UserScreenName],
                fullName = it.UserScreenName,
                displayName = it.UserScreenName,
                contestSystemId = it.UserScreenName,
                groups = emptyList(),
                hashTag = null,
                medias = emptyMap(),
                isHidden = false,
                isOutOfContest = false,
                organizationId = null,
            )
        }.sortedBy { it.id }

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
                val teamId = teamIds[teamResult.UserScreenName]
                for ((problemCdsId, problemResult) in teamResult.TaskResults) {
                    val problemId = problemIds[problemCdsId]
                    runs[teamId to problemId] = addNewRuns(teamId, problemId, problemResult).also {
                        addAll(it)
                    }
                }
            }
        }
        return ContestParseResult(info, newRuns, emptyList())
    }
}