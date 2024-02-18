@file:JvmMultifileClass
@file:JvmName("Adapters")

package org.icpclive.cds.adapters

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.icpclive.cds.*
import org.icpclive.cds.api.*

public class ContestState internal constructor(
    public val event: ContestUpdate,
    public val infoBeforeEvent: ContestInfo?,
    public val runs: PersistentMap<Int, RunInfo>,
    public val analyticsMessages: PersistentMap<String, AnalyticsMessage>,
) {
    public val infoAfterEvent: ContestInfo?
        get() = if (event is InfoUpdate) event.newInfo else infoBeforeEvent
}

public fun Flow<ContestUpdate>.contestState(): Flow<ContestState> = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<Int, RunInfo>()
    var curMessages = persistentMapOf<String, AnalyticsMessage>()
    collect {
        emit(ContestState(it, curInfo, curRuns, curMessages))
        when (it) {
            is RunUpdate -> curRuns = curRuns.put(it.newInfo.id, it.newInfo)
            is InfoUpdate -> curInfo = it.newInfo
            is AnalyticsUpdate -> curMessages = curMessages.put(it.message.id, it.message)
        }
    }
}