package org.icpclive

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.icpclive.api.*
import org.icpclive.events.*
import org.icpclive.events.RunInfo as ContestRunInfo

/**
 * Everything published here should be immutable, to allow secure work from many threads
 *
 * So for now, we can't do flow of contestInfo, we need to refactor it first.
 * Only runs are published now, with copy of list to make this data immutable
 */
object DataBus {
    // just wrapper for suspend world for java
    fun publishContestInfo(contestInfo: ContestInfo) = runBlocking {
        runsStorageUpdates.emit(contestInfo.runs.toList())
    }
    //TODO: this should be replaced with ContestInfo flow
    val runsStorageUpdates = MutableSharedFlow<List<ContestRunInfo>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val runsUpdates = MutableSharedFlow<RunInfo>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mainScreenEvents = MutableSharedFlow<MainScreenEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val queueEvents = MutableSharedFlow<QueueEvent>(
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    val allEvents get() = merge(mainScreenEvents, queueEvents)
}