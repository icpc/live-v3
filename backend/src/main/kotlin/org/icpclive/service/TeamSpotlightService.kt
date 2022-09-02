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
class TeamScoreboardPlace(val rank: Int, val contestTime: Duration) : TeamAccent()

class TeamState(val teamId: Int, private val flowSettings: TeamSpotlightFlowSettings) : Comparable<TeamState> {
    val score
        get() = internalScore
    private val accents = mutableSetOf<TeamAccent>()
    private var internalScore = 0.0
    private var causedRun: RunInfo? = null
    val caused: KeyTeamCause
        get() = causedRun?.let { RunCause(it.id) } ?: ScoreSumCause


    private fun Double.takeIf(condition: Boolean?) = if (condition == true) this else 0.0

    private fun calcScore() {
        var accentedFTSRun: RunInfo? = null
        var accentedACRun: RunInfo? = null
        var accentedRun: RunInfo? = null
        var newScore = 0.0
        for (accent in accents) {
            if (accent is TeamRunAccent) {
                accent.run.takeIf { it.isFirstSolvedRun }?.let { accentedFTSRun = it }
                accent.run.takeIf { it.isAccepted }?.let { accentedACRun = it }
                accentedRun = accent.run
            }
            if (accent is TeamScoreboardPlace) {
                newScore += flowSettings.rankScore(accent.rank)
            }
        }

        causedRun = (accentedFTSRun ?: accentedACRun ?: accentedRun)
        internalScore = newScore.takeIf(causedRun == null) +
                flowSettings.firstToSolvedRunScore.takeIf(causedRun?.isFirstSolvedRun) +
                flowSettings.acceptedRunScore.takeIf(causedRun?.isAccepted) +
                flowSettings.judgedRunScore.takeIf(causedRun != null)
    }

    fun cleanExpired(contestTime: Duration) {
        accents.removeIf {
            it is TeamRunAccent && it.run.time + flowSettings.runRelevance < contestTime ||
                    it is TeamScoreboardPlace && it.contestTime + flowSettings.scoreboardPushInterval < contestTime
        }
        calcScore()
    }

    fun addAccent(accent: TeamAccent) {
        accents += accent
        calcScore()
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

    fun getFlow(interval: Duration, settings: TeamSpotlightFlowSettings = TeamSpotlightFlowSettings()): Flow<KeyTeam> {
        val queue = mutableSetOf<TeamState>()
        fun getTeamInQueue(teamId: Int) =
            queue.find { it.teamId == teamId } ?: TeamState(teamId, settings).also { queue += it }

        scope.launch {
            val scoreboardFlow = scoreboardFlow.await()
            var previousCleanTime = Duration.ZERO
            var previousScoreboardPushTime = Duration.ZERO
            actionsFlow.collect { action ->
                when (action) {
                    is TriggerAction -> {
                        if (action.contestTime - previousCleanTime > settings.cleanInterval) {
                            for (team in queue) {
                                team.cleanExpired(action.contestTime)
                            }
                            previousCleanTime = action.contestTime
                        }
                        if (action.contestTime - previousScoreboardPushTime > settings.scoreboardPushInterval) {
                            scoreboardFlow.first().rows.filter { it.rank <= settings.scoreboardLowestRank }.forEach {
                                getTeamInQueue(it.teamId).addAccent(TeamScoreboardPlace(it.rank, action.contestTime))
                            }
                            for (team in queue) {
                                team.cleanExpired(action.contestTime)
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
                if (element.score == 0.0) {
                    continue
                }
                emit(KeyTeam(element.teamId, element.caused))
                delay(interval)
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
        var contestInfo = info.value
        actionsFlow.emit(TriggerAction(contestInfo.currentContestTime))
        merge(tickerFlow(5.seconds).map { Trigger }, info, runs).collect { update ->
            when (update) {
                is ContestInfo -> {
                    contestInfo = update
                }
                is RunInfo -> {
                    if (update.isJudged) {
                        actionsFlow.emit(AddScoreAction(update.teamId, TeamRunAccent(update)))
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
