package org.icpclive

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import org.icpclive.api.*
import org.icpclive.cds.OptimismLevel
import org.icpclive.cds.ContestInfo as EventsContestInfo

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
        scoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.NORMAL))
        optimisticScoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.OPTIMISTIC))
        pessimisticScoreboardFlow.value = Scoreboard(contestInfo.getStandings(OptimismLevel.PESSIMISTIC))
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

    val allEvents
        get() = merge(mainScreenEvents, queueEvents)
}