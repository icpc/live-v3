package org.icpclive.cds.adapters

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.*
import org.icpclive.cds.*
import org.icpclive.cds.api.*

public fun ContestState?.applyEvent(event: ContestUpdate): ContestState {
    val infoBeforeEvent = this?.infoAfterEvent
    var info: ContestInfo? = infoBeforeEvent
    val runsBeforeEvent = this?.runsAfterEvent ?: persistentMapOf()
    var runs: PersistentMap<RunId, RunInfo> = runsBeforeEvent
    val commentaryMessagesMessagesBeforeEvent = this?.commentaryMessagesAfterEvent ?: persistentMapOf()
    var commentaryMessagesMessages: PersistentMap<CommentaryMessageId, CommentaryMessage> = commentaryMessagesMessagesBeforeEvent
    when (event) {
        is InfoUpdate -> info = event.newInfo
        is RunUpdate -> runs = runs.put(event.newInfo.id, event.newInfo)
        is CommentaryMessagesUpdate -> commentaryMessagesMessages = commentaryMessagesMessages.put(event.message.id, event.message)
    }
    return ContestState(
        event,
        infoBeforeEvent,
        info,
        runsBeforeEvent,
        runs,
        commentaryMessagesMessagesBeforeEvent,
        commentaryMessagesMessages
    )
}

public fun Flow<ContestUpdate>.contestState(): Flow<ContestState> = transformContestState { emit(it) }

public fun <T> Flow<T>.transformContestState(block: suspend FlowCollector<ContestUpdate>.(T) -> Unit): Flow<ContestState> =
    flow {
        var curContestState: ContestState? = null
        transform(block).collect { value ->
            curContestState?.let {
                val isUseless = when (value) {
                    is RunUpdate -> it.runsAfterEvent[value.newInfo.id] == value.newInfo
                    is InfoUpdate -> it.infoAfterEvent == value.newInfo
                    is CommentaryMessagesUpdate -> it.commentaryMessagesAfterEvent[value.message.id] == value.message
                }
                if (isUseless) return@collect
            }
            curContestState = curContestState.applyEvent(value)
            curContestState?.let { emit(it) }
        }
    }
