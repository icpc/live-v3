package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public data class ScoreboardRowProblem(
    public val problemId: String? = null,
    public val numJudged: Int? = null,
    public val numPending: Int? = null,
    public val solved: Boolean? = null,
    @SinceClics(FeedVersion.`2022_07`)
    public val score: Double? = null,
    @LongMinutesBefore(FeedVersion.`2026_01`)
    public val time: Duration? = null
)
