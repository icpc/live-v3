@file:Suppress("UNUSED")
package org.icpclive.export.reactions

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.Exporter
import org.icpclive.Router
import org.icpclive.cds.CommentaryMessagesUpdate
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.Ranking
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import org.icpclive.cds.util.shareWith
import kotlin.time.Duration

@Serializable
class ShortRun(
    val id: RunId,
    val verdict: String,
    val isAccepted: Boolean,
    val problemId: ProblemId,
    val teamId: TeamId,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val isFirstToSolve: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val rankAfter: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val rankBefore: Int? = null,
)

@Serializable
class ShortTeamInfo(
    val id: TeamId,
    val fullName: String,
    val displayName: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val isOutOfContest: Boolean = false,
)

@Serializable
class ShortContestInfo(
    val name: String,
    val status: ContestStatus,
    val resultType: ContestResultType,
    @SerialName("contestLengthMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val contestLength: Duration,
    @SerialName("freezeTimeMs")
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val freezeTime: Duration?,
    val problems: List<ProblemInfo>,
    val teams: List<ShortTeamInfo>,
    val customFields: Map<String, String>,
)

@Serializable
class FullReactionsRunInfo(
    val id: String,
    val result: RunResult,
    val problem: ProblemInfo,
    val team: FullReactionsTeamInfo,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val testedTime: Duration? = null,
    @Required val reactionVideos: List<MediaType> = emptyList(),
)

@Serializable
class FullReactionsTeamInfo(
    val id: String,
    val fullName: String,
    val displayName: String,
    val groups: List<GroupInfo>,
    val hashTag: String?,
    val medias: Map<TeamMediaType, MediaType>,
    val isOutOfContest: Boolean,
    val scoreboardRowBefore: ScoreboardRow,
    val rankBefore: Int,
    val scoreboardRowAfter: ScoreboardRow,
    val rankAfter: Int,
    val color: Color?,
    @Required val organization: OrganizationInfo?,
    @Required val customFields: Map<String, String> = emptyMap(),
)

private fun RunInfo.toFullReactionsRun(contestState: ContestStateWithScoreboard): FullReactionsRunInfo? {
    if (isHidden) return null
    val info = contestState.state.infoAfterEvent ?: return null
    return FullReactionsRunInfo(
        id = id.value,
        result = result,
        problem = info.problems[problemId] ?: return null,
        team = info.teams[teamId]?.toFullReactionsTeam(contestState) ?: return null,
        time = time,
        testedTime = testedTime,
        reactionVideos = reactionVideos,
    )
}

private fun Ranking.getTeamRank(id: TeamId) : Int? {
    val listId = order.indexOfFirst { id == it }
    if (listId == -1) return null
    return ranks[listId]
}

private fun TeamInfo.toFullReactionsTeam(contestState: ContestStateWithScoreboard) : FullReactionsTeamInfo? {
    if (isHidden) return null
    val contestInfo = contestState.state.infoAfterEvent ?: return null
    return FullReactionsTeamInfo(
        id = id.value,
        fullName = fullName,
        displayName = displayName,
        groups = groups.mapNotNull { contestInfo.groups[it] },
        hashTag = hashTag,
        medias = medias,
        isOutOfContest = isOutOfContest,
        organization = organizationId?.let { contestInfo.organizations[organizationId] },
        customFields = customFields,
        color = color,
        scoreboardRowBefore = contestState.scoreboardRowBeforeOrNull(id) ?: return null,
        rankBefore = contestState.rankingBefore.getTeamRank(id) ?: return null,
        scoreboardRowAfter = contestState.scoreboardRowAfterOrNull(id) ?: return null,
        rankAfter = contestState.rankingAfter.getTeamRank(id) ?: return null
    )
}

inline fun <T> Flow<ContestStateWithScoreboard>.toRunsMap(crossinline convert: (RunInfo, ContestStateWithScoreboard) -> T) = flow {
    var runs = persistentMapOf<RunId, T>()
    collect {
        when (val e = it.state.lastEvent) {
            is CommentaryMessagesUpdate, is InfoUpdate -> {}
            is RunUpdate -> {
                if (e.newInfo.isHidden) {
                    runs = runs.remove(e.newInfo.id)
                } else {
                    runs = runs.put(e.newInfo.id, convert(e.newInfo, it))
                }
                emit(runs)
            }
        }
    }
}

private fun TeamInfo.toShortTeamInfo() = ShortTeamInfo(
    id = id,
    fullName = fullName,
    displayName = displayName,
    isOutOfContest = isOutOfContest
)

private fun ContestInfo.toShortContestInfo() = ShortContestInfo(
    name = name,
    status = status,
    resultType = resultType,
    contestLength = contestLength,
    freezeTime = freezeTime,
    problems = problems.values.toList(),
    teams = teams.values.filterNot { it.isHidden }.map { it.toShortTeamInfo() },
    customFields = customFields
)

private fun RunInfo.toShortRun(state: ContestStateWithScoreboard): ShortRun {
    val (verdict, isAccepted, isFirstToSolve) = when (val result = result) {
        is RunResult.ICPC -> Triple(result.verdict.shortName, result.verdict.isAccepted, result.isFirstToSolveRun)
        is RunResult.IOI -> Triple(result.wrongVerdict?.shortName ?: result.score.toString(), result.wrongVerdict == null, result.isFirstBestRun)
        is RunResult.InProgress -> Triple("IN_PROGRESS", false, false)
    }
    return ShortRun(
        id = id,
        verdict = verdict,
        isAccepted = isAccepted,
        isFirstToSolve = isFirstToSolve,
        problemId = problemId,
        teamId = teamId,
        time = time,
        rankBefore = if (isAccepted) state.rankingBefore.getTeamRank(teamId) else null,
        rankAfter = if (isAccepted) state.rankingAfter.getTeamRank(teamId) else null,
    )
}


object ReactionsExporter : Exporter {
    override val subscriptionCount: Int
        get() = 3
    override fun CoroutineScope.runOn(contestUpdates: Flow<ContestStateWithScoreboard>): Router {
        val stateFlow =
            contestUpdates
                .mapNotNull { it.state.infoAfterEvent?.toShortContestInfo() }
                .stateIn(this, SharingStarted.Eagerly, null)
                .filterNotNull()

        val shortRuns = contestUpdates
            .toRunsMap { run, state -> run.toShortRun(state) }
            .stateIn(this, SharingStarted.Eagerly, persistentMapOf())
        val fullRuns = contestUpdates
            .toRunsMap { run, info -> run.toFullReactionsRun(info) }
            .stateIn(this, SharingStarted.Eagerly, persistentMapOf())

        return Router {
            route("/reactions") {
                get {
                    call.respondText(
                        """
                    <html>
                    <body>
                    <a href="/reactions/contestInfo.json">contestInfo.json</a> <br/>
                    <a href="/reactions/runs.json">runs.json</a> <br/>
                    <a href="/reactions/fullRuns.json">fullRuns.json</a> <br/>
                    <a href="/reactions/fullRuns/id">Full run for run id</a> <br/>
                    </body>
                    </html>
                """.trimIndent(),
                        ContentType.Text.Html
                    )
                }
                get("/contestInfo.json") {
                    call.respond(stateFlow.first())
                }
                get("/runs.json") {
                    call.respond(shortRuns.first().values.toList())
                }
                get("/fullRuns.json") {
                    call.respond(fullRuns.first().values.toList())
                }
                get("/fullRuns/{id}") {
                    val runInfo = fullRuns.first()[call.parameters["id"]!!.toRunId()]
                    if (runInfo == null) {
                        call.respond(HttpStatusCode.NotFound)
                    } else {
                        call.respond(runInfo)
                    }
                }
            }
        }
    }
}