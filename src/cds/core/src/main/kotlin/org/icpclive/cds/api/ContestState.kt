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
    public val analyticsMessagesBeforeEvent: PersistentMap<String, CommentaryMessage>,
    public val analyticsMessagesAfterEvent: PersistentMap<String, CommentaryMessage>,
)

public class ContestStateBuilder(private val event: ContestUpdate, previousState: ContestState?) {
    private val infoBeforeEvent = previousState?.infoAfterEvent
    public var info: ContestInfo? = infoBeforeEvent
    private var runsBeforeEvent = previousState?.runsAfterEvent ?: persistentMapOf()
    public var runs: PersistentMap<RunId, RunInfo> = runsBeforeEvent
    private var analyticsMessagesBeforeEvent = previousState?.analyticsMessagesAfterEvent ?: persistentMapOf()
    public var analyticsMessages: PersistentMap<String, CommentaryMessage> = analyticsMessagesBeforeEvent

    public fun build() : ContestState = ContestState(
        event,
        infoBeforeEvent,
        info,
        runsBeforeEvent,
        runs,
        analyticsMessagesBeforeEvent,
        analyticsMessages
    )
}

public fun ContestState(event: ContestUpdate, previousState: ContestState?, configure: ContestStateBuilder.() -> Unit) : ContestState {
    return ContestStateBuilder(event, previousState).apply {
        configure()
    }.build()
}
