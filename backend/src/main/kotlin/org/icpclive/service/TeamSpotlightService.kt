package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.icpclive.api.ContestInfo
import org.icpclive.api.KeyTeam
import org.icpclive.api.RunInfo
import org.icpclive.utils.getLogger
import org.icpclive.utils.tickerFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


private sealed class ServiceAction
private class CleanAction(val contestTime: Duration) : ServiceAction()
private class AddScoreAction(val teamId: Int, val accent: TeamAccent) : ServiceAction()

sealed class TeamAccent
class TeamRunAccent(val run: RunInfo) : TeamAccent()
class TeamScoreboardPlace() : TeamAccent()

class TeamState(val teamId: Int) : Comparable<TeamState> {
    val score
        get() = internalScore
    private val accents = mutableSetOf<TeamAccent>()
    private var internalScore = 0
    private var causedRun: RunInfo? = null
    val caused: String
        get() {
            if (causedRun?.isFirstSolvedRun == true) {
                return "FTS run"
            }
            if (causedRun?.isAccepted == true) {
                return "AC run"
            }
            if (causedRun?.isFirstSolvedRun == true) {
                return "Other run"
            }
            return ""
        }

    private fun Int.takeIf(condition: Boolean?) = if (condition == true) this else 0

    private fun calcScore() {
        var accentedFTSRun: RunInfo? = null
        var accentedACRun: RunInfo? = null
        var accentedRun: RunInfo? = null
        val newScore = 0
        for (accent in accents) {
            if (accent is TeamRunAccent) {
                accent.run.takeIf { it.isFirstSolvedRun }?.let { accentedFTSRun = it }
                accent.run.takeIf { it.isAccepted }?.let { accentedACRun = it }
                accentedRun = accent.run
            }
        }

        causedRun = (accentedFTSRun ?: accentedACRun ?: accentedRun)
        internalScore = newScore +
                10.takeIf(causedRun?.isFirstSolvedRun) +
                5.takeIf(causedRun?.isAccepted) +
                5.takeIf(causedRun != null)
    }

    fun cleanExpired(contestTime: Duration) {
        accents.removeIf { it is TeamRunAccent && it.run.time + 2.minutes < contestTime }
        calcScore()
    }

    fun addAccent(accent: TeamAccent) {
        accents += accent
        calcScore()
    }

    override fun compareTo(other: TeamState): Int =
        (other.score - score).takeIf { it != 0 } ?: (teamId - other.teamId)
}

class TeamSpotlightService(val scope: CoroutineScope) {
    private val actionsFlow = MutableSharedFlow<ServiceAction>(extraBufferCapacity = 10000)

    fun getFlow(interval: Duration): Flow<KeyTeam> {
        val queue = mutableSetOf<TeamState>()
        scope.launch {
            actionsFlow.collect { action ->
                when (action) {
                    is CleanAction -> {
                        for (team in queue) {
                            team.cleanExpired(action.contestTime)
                        }
                    }
                    is AddScoreAction -> {
                        val team = queue.find { it.teamId == action.teamId }
                            ?: TeamState(action.teamId).also { queue += it }
                        team.addAccent(action.accent)
                    }
                }
            }
        }
        return flow {
            var previousUpdateTime = Instant.Companion.fromEpochMilliseconds(0)
            while (true) {
                val element = queue.minOrNull()
                if (element == null) {
                    delay(1.seconds)
                    continue
                }
                queue.remove(element)
                if (element.score == 0) {
                    continue
                }
                emit(KeyTeam(element.teamId, element.caused))
                val updateTime = Clock.System.now()
                delay(previousUpdateTime + interval - updateTime)
                previousUpdateTime = updateTime
            }
        }
    }

    suspend fun run(
        info: MutableStateFlow<ContestInfo>,
        runs: Flow<RunInfo>,
        // analyticsMessage: Flow<AnalyticsMessage>
    ) {
        var contestInfo = info.value
        merge(tickerFlow(15.seconds).map { CleanTrigger }, info, runs).collect { update ->
            when (update) {
                is ContestInfo -> {
                    contestInfo = update
                }
                is RunInfo -> {
                    if (update.isJudged) {
                        actionsFlow.emit(AddScoreAction(update.teamId, TeamRunAccent(update)))
                    }
                }
                is CleanTrigger -> {
                    actionsFlow.emit(CleanAction(contestInfo.currentContestTime))
                }
            }
        }
    }

    companion object {
        private object CleanTrigger

        val logger = getLogger(TeamSpotlightService::class)
    }
}
