package org.icpclive.cds.api

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.icpclive.cds.ContestUpdate

public class ContestState internal constructor(
    public val lastEvent: ContestUpdate,
    public val infoBeforeEvent: ContestInfo?,
    public val infoAfterEvent: ContestInfo?,
    public val runsBeforeEvent: PersistentMap<RunId, RunInfo>,
    public val runsAfterEvent: PersistentMap<RunId, RunInfo>,
    public val commentaryMessagesBeforeEvent: PersistentMap<CommentaryMessageId, CommentaryMessage>,
    public val commentaryMessagesAfterEvent: PersistentMap<CommentaryMessageId, CommentaryMessage>,
)

public class ContestStateBuilder(private val event: ContestUpdate, previousState: ContestState?) {
    private val infoBeforeEvent = previousState?.infoAfterEvent
    public var info: ContestInfo? = infoBeforeEvent
    private var runsBeforeEvent = previousState?.runsAfterEvent ?: persistentMapOf()
    public var runs: PersistentMap<RunId, RunInfo> = runsBeforeEvent
    private var commentaryMessagesBeforeEvent = previousState?.commentaryMessagesAfterEvent ?: persistentMapOf()
    public var commentaryMessages: PersistentMap<CommentaryMessageId, CommentaryMessage> = commentaryMessagesBeforeEvent

    public fun build() : ContestState = ContestState(
        event,
        infoBeforeEvent,
        info,
        runsBeforeEvent,
        runs,
        commentaryMessagesBeforeEvent,
        commentaryMessages
    )
}

public fun ContestState(event: ContestUpdate, previousState: ContestState?, configure: ContestStateBuilder.() -> Unit) : ContestState {
    return ContestStateBuilder(event, previousState).apply {
        configure()
    }.build()
}
