package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import org.icpclive.cds.CommentaryMessagesUpdate
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.applyEvent
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.api.CommentaryMessage
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.ContestState
import org.icpclive.cds.api.toProblemId
import org.icpclive.cds.api.toTeamId
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.getLogger


internal fun generateCommentary(
    flow: Flow<ContestStateWithScoreboard>,
    generator: (ContestStateWithScoreboard) -> List<CommentaryMessage>
) : Flow<ContestStateWithScoreboard> {
    var contestState: ContestState? = null
    return flow.transform {
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

private val teamRegex = Regex("\\{ *teams? *: *([^}]+) *}")
private val problemsRegex = Regex("\\{ *problems? *: *([^}]+) *}")
private val logger by getLogger()

internal fun processCommentaryTags(flow: Flow<ContestUpdate>): Flow<ContestUpdate> {
    fun String.processMessageTags(info: ContestInfo) : String {
        return this
            .replace(teamRegex) {
                val teamId = it.groupValues[1].toTeamId()
                val team = info.teams[teamId]
                if (team != null) {
                    team.displayName
                } else {
                    logger.warning { "Unknown team id $teamId in commentary" }
                    it.value
                }
            }
            .replace(problemsRegex) {
                val problemId = it.groupValues[1].toProblemId()
                val problem = info.problems[problemId]
                if (problem != null) {
                    problem.displayName
                } else {
                    logger.warning { "Unknown problem id $problemId in commentary" }
                    it.value
                }
            }
    }
    return flow.contestState().map {
        when (it.lastEvent) {
            is CommentaryMessagesUpdate -> {
                if (it.infoAfterEvent == null)
                    it.lastEvent
                else
                    CommentaryMessagesUpdate(
                        it.lastEvent.message.copy(
                            message = it.lastEvent.message.message.processMessageTags(it.infoAfterEvent)
                        )
                    )
            }

            else -> it.lastEvent
        }
    }
}