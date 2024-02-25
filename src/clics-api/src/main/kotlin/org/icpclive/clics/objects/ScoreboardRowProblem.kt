package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface ScoreboardRowProblem {
    @Required
    public val problemId: String
    @Required
    public val numJudged: Int
    @Required
    public val numPending: Int
    @Required
    public val solved: Boolean
    public val time: Long?
}