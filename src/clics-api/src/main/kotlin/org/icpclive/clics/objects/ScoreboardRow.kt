package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface ScoreboardRow {
    @Required
    public val rank: Int
    @Required
    public val teamId: String
    @Required
    public val score: ScoreboardRowScore
    @Required
    public val problems: List<ScoreboardRowProblem>
}