package org.icpclive.cds.plugins.dmoj

import kotlinx.datetime.Instant
import kotlinx.serialization.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.ksp.cds.Builder
import kotlin.time.Duration.Companion.seconds

@Builder("dmoj")
public sealed interface DmojSettings : CDSSettings {
    public val url: String
    public val contestId: String
    public val apiKey: Credential

    override fun toDataSource(): ContestDataSource = DmojDataSource(this)
}

@Serializable
private class Error(val code: Int, val message: String)

@Serializable
private class Wrapper<T>(
    val data: T? = null,
    val error: Error? = null
) {
    fun unwrap(): T {
        if (error != null) throw IllegalStateException("Dmoj returned error: $error")
        return data!!
    }
}

@Serializable
private class ContestResponse(
    val `object`: Contest
)

@Serializable
private class Contest(
    val name: String,
    val start_time: Instant,
    val end_time: Instant,
    val time_limit: Double?,
    val format: Format,
    val problems: List<Problem>,
    val rankings: List<User>
)

@Serializable
private class Problem(
    val points: Int,
    val label: String,
    val name: String,
    val code: String,
)

@Serializable
private class User(
    val user: String,
    val start_time: Instant?,
    val is_disqualified: Boolean?
)

@Serializable
private class Format(val name: String)

@Serializable
private class SubmissionsResult(
    val has_more: Boolean,
    val objects: List<Submission>,
)

@Serializable
private class Submission(
    val id: Int,
    val problem: String,
    val user: String,
    val date: Instant,
    val points: Double?,
    val result: String
)

internal class DmojDataSource(val settings: DmojSettings) : FullReloadContestDataSource(5.seconds) {

    val apiBaseUrl = UrlOrLocalPath.Url(settings.url).subDir("api/v2")
        .withBearer(settings.apiKey)

    private val contestInfoLoader = DataLoader.json<Wrapper<ContestResponse>>(
        settings.network,
        null,
        apiBaseUrl.subDir("contest").subDir(settings.contestId)
    ).map { it.unwrap().`object` }

    override suspend fun loadOnce(): ContestParseResult {
        val contest = contestInfoLoader.load()

        val contestLength = contest.time_limit?.seconds ?: (contest.end_time - contest.start_time)

        val resultType = when (contest.format.name) {
            "icpc" -> ContestResultType.ICPC
            "ioi" -> ContestResultType.IOI
            else -> error("Unknown contest format: ${contest.format.name}")
        }
        val startTimeMap = mutableMapOf<TeamId, Instant>()
        val info = ContestInfo(
            name = contest.name,
            status = ContestStatus.byCurrentTime(contest.start_time, contestLength),
            resultType = resultType,
            startTime = contest.start_time,
            contestLength = contestLength,
            freezeTime = contestLength,
            penaltyRoundingMode = when (resultType) {
                ContestResultType.ICPC -> PenaltyRoundingMode.SUM_IN_SECONDS
                ContestResultType.IOI -> PenaltyRoundingMode.ZERO
            },
            groupList = emptyList(),
            teamList = contest.rankings.map {
                TeamInfo(
                    id = it.user.toTeamId(),
                    displayName = it.user,
                    fullName = it.user,
                    groups = emptyList(),
                    hashTag = null,
                    medias = emptyMap(),
                    isHidden = it.is_disqualified == true,
                    isOutOfContest = false,
                    organizationId = null
                ).also { team ->
                    startTimeMap[team.id] = it.start_time ?: contest.start_time
                }
            },
            organizationList = emptyList(),
            problemList = contest.problems.mapIndexed { index, it ->
                ProblemInfo(
                    id = it.code.toProblemId(),
                    displayName = it.label,
                    fullName = it.name,
                    ordinal = index,
                    minScore = if (resultType == ContestResultType.IOI) 0.0 else null,
                    maxScore = if (resultType == ContestResultType.IOI) it.points.toDouble() else null,
                    scoreMergeMode = if (resultType == ContestResultType.IOI) ScoreMergeMode.LAST_OK else null
                )
            }
        )
        val submissions = buildList {
            for (problem in contest.problems) {
                var page = 0
                val loader = DataLoader.json<Wrapper<SubmissionsResult>>(
                    settings.network,
                ) {
                    apiBaseUrl.subDir("submissions?problem=${problem.code}&page=$page")
                }.map { it.unwrap() }
                while (true) {
                    ++page
                    val data = loader.load()
                    for (submission in data.objects) {
                        val userStartTime = startTimeMap[submission.user.toTeamId()] ?: continue
                        val time = submission.date - userStartTime
                        if (time > contestLength) continue
                        val verdict = Verdict.lookup(
                            shortName = submission.result,
                            isAddingPenalty = submission.result != "CE" && submission.result != "AC",
                            isAccepted = submission.result == "AC"
                        )
                        val result = when (resultType) {
                            ContestResultType.ICPC -> {
                                verdict.toICPCRunResult()
                            }
                            ContestResultType.IOI -> RunResult.IOI(
                                listOf(submission.points ?: 0.0),
                                wrongVerdict = verdict.takeIf { submission.points == null }
                            )
                        }
                        add(RunInfo(
                            id = submission.id.toRunId(),
                            result = result,
                            problemId = submission.problem.toProblemId(),
                            teamId = submission.user.toTeamId(),
                            time = time
                        ))
                    }
                    if (!data.has_more) break
                }
            }
        }
        return ContestParseResult(info, submissions, emptyList())
    }
}
