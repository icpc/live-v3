package org.icpclive

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.icpclive.api.*
import org.icpclive.events.OptimismLevel
import org.icpclive.events.ContestInfo as EventsContestInfo

/**
 * Everything published here should be immutable, to allow secure work from many threads
 *
 * So for now, we can't do flow of contestInfo, we need to refactor it first.
 * Only runs are published now, with copy of list to make this data immutable
 */
object DataBus {
    // just wrapper for suspend world for java
    fun publishContestInfo(contestInfo: EventsContestInfo) = runBlocking {
        runsStorageUpdates.emit(contestInfo.runs.map { it.toApi() })
        contestInfoFlow.value = contestInfo.toApi()
        //TODO: rework to avoid triple computation event it's not needed
        scoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.NORMAL).map { ScoreboardRow(it) })
        optimisticScoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.OPTIMISTIC).map { ScoreboardRow(it) })
        pessimisticScoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.PESSIMISTIC).map { ScoreboardRow(it) })
    }
    val contestInfoFlow = MutableStateFlow(ContestInfo.EMPTY)
    //TODO: this should be replaced with ContestInfo flow
    val runsStorageUpdates = MutableSharedFlow<List<RunInfo>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mainScreenEvents = MutableSharedFlow<MainScreenEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val queueEvents = MutableSharedFlow<QueueEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val scoreboardFlow = MutableStateFlow(Scoreboard(emptyList()))
    val optimisticScoreboardFlow = MutableStateFlow(Scoreboard(emptyList()))
    val pessimisticScoreboardFlow = MutableStateFlow(Scoreboard(emptyList()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val allEvents get() = merge(mainScreenEvents, queueEvents)
}