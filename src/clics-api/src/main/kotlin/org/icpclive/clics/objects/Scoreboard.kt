package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public data class Scoreboard(
    @Required public val time: Instant,
    @Required public val contestTime: Duration,
    @Required public val state: State,
    @Required public val rows: List<ScoreboardRow>
)
