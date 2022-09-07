package org.icpclive.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.icpclive.api.*
import org.icpclive.utils.completeOrThrow
import org.icpclive.utils.getLogger
import org.icpclive.utils.tickerFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


private sealed class ServiceAction
private class TriggerAction(val contestTime: Duration) : ServiceAction()
private class AddScoreAction(val teamId: Int, val accent: TeamAccent) : ServiceAction()

sealed class TeamAccent
class TeamRunAccent(val run: RunInfo) : TeamAccent()
class TeamScoreboardPlace(val rank: Int) : TeamAccent()

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
    is TeamScoreboardPlace -> flowSettings.rankScore(rank)
    is TeamRunAccent -> flowSettings.firstToSolvedRunScore.takeIf(run.isFirstSolvedRun) +
            flowSettings.acceptedRunScore.takeIf(run.isAccepted) +
            flowSettings.judgedRunScore.takeIf(run.isJudged) +
            flowSettings.notJudgedRunScore.takeIf(!run.isJudged)
}

class TeamState(val teamId: Int, private val flowSettings: TeamSpotlightFlowSettings) : Comparable<TeamState> {
    val score
        get() = internalScore
    private var internalScore = 0.0
    private var causedRun: RunInfo? = null
    val caused: KeyTeamCause
        get() = causedRun?.let { RunCause(it.id) } ?: ScoreSumCause

    fun addAccent(accent: TeamAccent) {
        internalScore += accent.getScoreDelta(flowSettings)
        if (accent is TeamRunAccent) {
            causedRun = causedRun?.coerceAtMost(accent.run) ?: accent.run
        }
    }

    override fun compareTo(other: TeamState): Int = when {
        other.score > score -> 1
        other.score < score -> -1
        else -> (teamId - other.teamId)
    }
}

class TeamSpotlightService(val scope: CoroutineScope) {
    private val actionsFlow = MutableSharedFlow<ServiceAction>(extraBufferCapacity = 10000)
    private val scoreboardFlow = CompletableDeferred<Flow<Scoreboard>>()

    fun getFlow(settings: TeamSpotlightFlowSettings = TeamSpotlightFlowSettings()): Flow<KeyTeam> {
        val queue = mutableSetOf<TeamState>()
        fun getTeamInQueue(teamId: Int) =
            queue.find { it.teamId == teamId } ?: TeamState(teamId, settings).also { queue += it }

        scope.launch {
            val scoreboardFlow = scoreboardFlow.await()
            var previousScoreboardPushTime = Duration.ZERO
            actionsFlow.collect { action ->
                when (action) {
                    is TriggerAction -> {
                        // Should we have different scoreboardPushInterval interval,
                        // or we can have PushScoreboardAction without universal Trigger?
                        if (action.contestTime - previousScoreboardPushTime > settings.scoreboardPushInterval) {
                            scoreboardFlow.first().rows.filter { it.rank <= settings.scoreboardLowestRank }.forEach {
                                getTeamInQueue(it.teamId).addAccent(TeamScoreboardPlace(it.rank))
                            }
                            previousScoreboardPushTime = action.contestTime
                        }
                    }
                    is AddScoreAction -> {
                        getTeamInQueue(action.teamId).addAccent(action.accent)
                    }
                }
            }
        }
        return flow {
            while (true) {
                val element = queue.minOrNull()
                if (element == null) {
                    delay(1.seconds)
                    continue
                }
                queue.remove(element)
                emit(KeyTeam(element.teamId, element.caused))
            }
        }
    }

    suspend fun run(
        info: MutableStateFlow<ContestInfo>,
        runs: Flow<RunInfo>,
        scoreboard: Flow<Scoreboard>,
        // analyticsMessage: Flow<AnalyticsMessage>
    ) {
        scoreboardFlow.completeOrThrow(scoreboard)
        val runIds = mutableSetOf<Int>()
        var contestInfo = info.value
        actionsFlow.emit(TriggerAction(contestInfo.currentContestTime))
        merge(
            tickerFlow(15.seconds).map { Trigger },
            info,
            runs
        ).collect { update ->
            when (update) {
                is ContestInfo -> {
                    contestInfo = update
                }
                is RunInfo -> {
                    if (update.time + 60.seconds > contestInfo.currentContestTime) {
                        if (update.isJudged || update.id !in runIds) {
                            runIds += update.id
                            actionsFlow.emit(AddScoreAction(update.teamId, TeamRunAccent(update)))
                        }
                    }
                }
                is Trigger -> {
                    actionsFlow.emit(TriggerAction(contestInfo.currentContestTime))
                }
            }
        }
    }

    companion object {
        private object Trigger

        val logger = getLogger(TeamSpotlightService::class)
    }
}
