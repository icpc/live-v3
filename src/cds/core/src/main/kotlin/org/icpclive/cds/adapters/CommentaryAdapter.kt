package org.icpclive.cds.adapters

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.CommentaryMessagesUpdate
import org.icpclive.cds.api.CommentaryMessage
import org.icpclive.cds.api.ContestState
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard


public fun Flow<ContestStateWithScoreboard>.generateCommentary(
    generator: (ContestStateWithScoreboard) -> List<CommentaryMessage>
) : Flow<ContestStateWithScoreboard> {
    var contestState: ContestState? = null
    return transform {
        contestState = contestState.applyEvent(it.state.lastEvent)
        emit(
            ContestStateWithScoreboard(
                state = contestState!!,
                scoreboardRowsAfter = it.scoreboardRowsAfter,
                scoreboardRowsBefore = it.scoreboardRowsBefore,
                scoreboardRowsChanged = it.scoreboardRowsChanged,
                rankingBefore = it.rankingBefore,
                rankingAfter = it.rankingAfter,
                lastSubmissionTime = it.lastSubmissionTime
            )
        )
        for (message in generator(it)) {
            val event = CommentaryMessagesUpdate(message)
            contestState = contestState.applyEvent(event)
            emit(
                ContestStateWithScoreboard(
                    state = contestState!!,
                    scoreboardRowsAfter = it.scoreboardRowsAfter,
                    scoreboardRowsBefore = it.scoreboardRowsAfter,
                    scoreboardRowsChanged = emptyList(),
                    rankingBefore = it.rankingAfter,
                    rankingAfter = it.rankingAfter,
                    lastSubmissionTime = it.lastSubmissionTime
                )
            )
        }
    }
}