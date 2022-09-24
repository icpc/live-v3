package org.icpclive.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*
import org.icpclive.utils.getLogger
import org.icpclive.utils.tickerFlow
import kotlin.time.Duration.Companion.seconds


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
    var score = 0.0
        private set
    private var causedRun: RunInfo? = null
    val caused: KeyTeamCause
        get() = causedRun?.let { RunCause(it.id) } ?: ScoreSumCause

    fun addAccent(accent: TeamAccent) {
        score += accent.getScoreDelta(flowSettings)
        if (accent is TeamRunAccent) {
            causedRun = causedRun?.coerceAtMost(accent.run) ?: accent.run
        }
    }

    override fun compareTo(other: TeamState): Int = when {
        other.score > score -> 1
        other.score < score -> -1
        else -> teamId.compareTo(other.teamId)
    }
}

class TeamSpotlightService(
    val scope: CoroutineScope,
    val settings: TeamSpotlightFlowSettings = TeamSpotlightFlowSettings()
) {
    private val mutex = Mutex()
    private val queue = mutableSetOf<TeamState>()
    private fun getTeamInQueue(teamId: Int) =
        queue.find { it.teamId == teamId } ?: TeamState(teamId, settings).also { queue += it }

    fun getFlow(): Flow<KeyTeam> {
        return flow {
            while (true) {
                val element = mutex.withLock {
                    queue.minOrNull()?.also { queue.remove(it) }
                }
                if (element == null) {
                    delay(1.seconds)
                    continue
                }
                emit(KeyTeam(element.teamId, element.caused))
            }
        }
    }

    suspend fun run(
        info: StateFlow<ContestInfo>,
        runs: Flow<RunInfo>,
        scoreboard: Flow<Scoreboard>,
        // analyticsMessage: Flow<AnalyticsMessage>
    ) {
        val runIds = mutableSetOf<Int>()
        merge(
            tickerFlow(settings.scoreboardPushInterval).map { ScoreboardPushTrigger },
            runs.filter { !it.isHidden }
        ).collect { update ->
            when (update) {
                is RunInfo -> {
                    if (update.time + 60.seconds > info.value.currentContestTime) {
                        if (update.isJudged || update.id !in runIds) {
                            runIds += update.id
                            mutex.withLock {
                                getTeamInQueue(update.teamId).addAccent(TeamRunAccent(update))
                            }
                        }
                    }
                }
                is ScoreboardPushTrigger -> {
                    scoreboard.first().rows.filter { it.rank <= settings.scoreboardLowestRank }.forEach {
                        mutex.withLock {
                            getTeamInQueue(it.teamId).addAccent(TeamScoreboardPlace(it.rank))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private object ScoreboardPushTrigger

        val logger = getLogger(TeamSpotlightService::class)
    }
}
