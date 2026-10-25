package org.icpclive.service

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.icpclive.api.*
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.scoreboard.toScoreboardDiff
import org.icpclive.cds.util.DeferredFlow
import org.icpclive.cds.util.loopFlow
import kotlin.time.Duration.Companion.seconds


sealed class TeamAccent
class TeamRunAccent(val run: RunInfo) : TeamAccent()
class TeamScoreboardPlace(val rank: Int, val contestStatus: ContestStatus) : TeamAccent()
class ExternalScoreAddAccent(val score: Double) : TeamAccent()
object SocialEventAccent : TeamAccent()

private object ScoreboardPushTrigger

private val RunInfo.isFirstSolvedRun
    get() = when (val result = this.result) {
        is RunResult.ICPC -> result.isFirstToSolveRun
        is RunResult.IOI -> result.isFirstBestRun
        is RunResult.InProgress -> false
    }
private val RunInfo.isAccepted
    get() = when (val result = this.result) {
        is RunResult.ICPC -> result.verdict.isAccepted
        is RunResult.IOI -> false
        is RunResult.InProgress -> false
    }
private val RunInfo.isJudged get() = result !is RunResult.InProgress

private fun RunInfo.coerceAtMost(other: RunInfo): RunInfo {
    if (interesting() <= other.interesting()) {
        return other
    }
    return this
}

private fun RunInfo.interesting() = when {
    isFirstSolvedRun -> 3
    isAccepted -> 2
    isJudged -> 1
    else -> 0
}

private fun Double.takeIf(condition: Boolean?) = if (condition == true) this else 0.0
private fun TeamAccent.getScoreDelta(flowSettings: TeamSpotlightFlowSettings) = when (this) {
    is TeamScoreboardPlace -> flowSettings.rankScore(rank, contestStatus)
    is TeamRunAccent -> {
        when (val result = run.result) {
            is RunResult.ICPC -> {
                flowSettings.firstToSolvedRunScore.takeIf(result.isFirstToSolveRun) +
                        flowSettings.acceptedRunScore.takeIf(result.verdict.isAccepted) +
                        flowSettings.judgedRunScore.takeIf(run.isFirstSolvedRun) +
                        flowSettings.notJudgedRunScore.takeIf(!run.isJudged)
            }

            is RunResult.IOI -> {
                flowSettings.acceptedRunScore.takeIf(result.difference != 0.0) +
                        flowSettings.judgedRunScore.takeIf(run.isFirstSolvedRun) +
                        flowSettings.notJudgedRunScore.takeIf(!run.isJudged)
            }

            is RunResult.InProgress -> 0.0
        }
    }

    is ExternalScoreAddAccent -> flowSettings.externalScoreScale * score
    is SocialEventAccent -> flowSettings.socialEventScore
}

@Serializable
class TeamState(val teamId: TeamId) : Comparable<TeamState> {
    var score = 0.0
        private set
    private var causedRun: RunInfo? = null
    val caused: KeyTeamCause
        get() = causedRun?.let { RunCause(it.id) } ?: ScoreSumCause

    fun addAccent(accent: TeamAccent, flowSettings: TeamSpotlightFlowSettings) {
        score += accent.getScoreDelta(flowSettings)
        if (accent is TeamRunAccent) {
            causedRun = causedRun?.coerceAtMost(accent.run) ?: accent.run
        }
    }

    override fun compareTo(other: TeamState): Int = when {
        other.score > score -> 1
        other.score < score -> -1
        else -> teamId.value.compareTo(other.teamId.value)
    }
}

class TeamSpotlightService(
    private val contestInfoFlow: Flow<ContestInfo>,
    private val scoreRequestFlow: Flow<AddTeamScoreRequest>,
    private val socialEventsFlow: Flow<SocialEvent>,
    teamSpotlightFlow: DeferredFlow<KeyTeam>,
    teamInterestingFlow: DeferredFlow<List<CurrentTeamState>>,
) : Service {
    private val teamInterestingMutableFlow = MutableStateFlow(emptyList<CurrentTeamState>())
        .also { teamInterestingFlow.complete(it) }

    val settings: TeamSpotlightFlowSettings = TeamSpotlightFlowSettings()
    private val mutex = Mutex()
    private val queue = mutableSetOf<TeamState>()
    private fun getTeamInQueue(teamId: TeamId) =
        queue.find { it.teamId == teamId } ?: TeamState(teamId).also { queue += it }

    init {
        teamSpotlightFlow.complete(flow {
            while (true) {
                val element = mutex.withLock {
                    queue.minOrNull()?.also { queue.remove(it) }
                }
                if (element == null) {
                    delay(1.seconds)
                    continue
                }
                val teamIsHidden = contestInfoFlow.first().teams[element.teamId]?.isHidden ?: true
                if (teamIsHidden) {
                    continue
                }
                emit(KeyTeam(element.teamId, element.caused))
                val teams = mutex.withLock { queue.map { CurrentTeamState(it.teamId, it.score) } }
                teamInterestingMutableFlow.emit(teams)
            }
        })
    }

    private suspend fun TeamState.addAccent(accent: TeamAccent) {
        addAccent(accent, settings)
        teamInterestingMutableFlow.emit(queue.map { CurrentTeamState(it.teamId, it.score) })
    }

    override fun CoroutineScope.runOn(flow: Flow<ContestStateWithScoreboard>) {
        val runIds = mutableSetOf<RunId>()
        launch {
            var scoreboardState: ScoreboardDiff? = null
            var lastInfo: ContestInfo? = null
            merge(
                loopFlow(settings.scoreboardPushInterval, onError = {}) { ScoreboardPushTrigger },
                flow.filter { state ->
                    state.state.infoAfterEvent?.status is ContestStatus.BEFORE ||
                            state.state.lastEvent.let { it is RunUpdate && !it.newInfo.isHidden }
                },
                scoreRequestFlow,
                socialEventsFlow,
            ).collect { update ->
                when (update) {
                    is ContestStateWithScoreboard -> {
                        scoreboardState = update.toScoreboardDiff(snapshot = true)
                        val info = update.state.infoAfterEvent ?: return@collect
                        lastInfo = info
                        val runUpdate = update.state.lastEvent as? RunUpdate ?: return@collect
                        val run = runUpdate.newInfo
                        if (run.time + 60.seconds > info.currentContestTime) {
                            if (run.isJudged || run.id !in runIds) {
                                runIds += run.id
                                mutex.withLock {
                                    getTeamInQueue(run.teamId).addAccent(TeamRunAccent(run))
                                }
                            }
                        }
                    }

                    is ScoreboardPushTrigger -> {
                        scoreboardState?.let { it ->
                            it.order.zip(it.ranks).takeWhile { it.second <= settings.scoreboardLowestRank }.forEach {
                                mutex.withLock {
                                    getTeamInQueue(it.first).addAccent(
                                        TeamScoreboardPlace(
                                            it.second,
                                            lastInfo?.status ?: ContestStatus.BEFORE()
                                        )
                                    )
                                }
                            }
                        }
                    }

                    is AddTeamScoreRequest -> {
                        mutex.withLock {
                            getTeamInQueue(update.teamId).addAccent(ExternalScoreAddAccent(update.score))
                        }
                    }

                    is SocialEvent -> {
                        update.teamIds.forEach { teamId ->
                            mutex.withLock {
                                getTeamInQueue(teamId).addAccent(SocialEventAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}
