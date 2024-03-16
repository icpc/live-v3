package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.cds.api.TeamId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed class KeyTeamCause
class RunCause(val runId: Int) : KeyTeamCause()
object ScoreSumCause : KeyTeamCause()

data class KeyTeam(val teamId: TeamId, val cause: KeyTeamCause)
data class CurrentTeamState(val teamId: TeamId, val score: Double)

@Serializable
data class InterestingTeam(val teamId: TeamId, val teamName: String, val score: Double)

@Serializable
data class AddTeamScoreRequest(val teamId: TeamId, val score: Double)

data class TeamSpotlightFlowSettings(
    val notJudgedRunScore: Double = 5.0,
    val judgedRunScore: Double = 10.0,
    val acceptedRunScore: Double = 5.0,
    val firstToSolvedRunScore: Double = 5.0,
    val runRelevance: Duration = 90.seconds,
    val cleanInterval: Duration = 15.seconds,
    val scoreboardPushInterval: Duration = 180.seconds,
    val scoreboardLowestRank: Int = 40,
    val scoreboardFirstScore: Double = 5.0,
    val scoreboardLastScore: Double = 1.0,
    val socialEventScore: Double = 100.0,
    val externalScoreScale: Double = 1.0,
) {
    fun rankScore(rank: Int): Double {
        if (rank > scoreboardLowestRank) {
            return 0.0
        }
        return scoreboardLastScore +
                (scoreboardLowestRank - rank) * (scoreboardFirstScore - scoreboardLastScore) / (scoreboardLowestRank - 1)
    }
}
