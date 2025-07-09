package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("judgements")
public data class Judgement(
    @Required public val id: String,
    @Required public val submissionId: String,
    @Required public val startContestTime: Duration,
    @Required public val startTime: Instant,
    public val endTime: Instant? = null,
    public val endContestTime: Duration? = null,
    public val judgementTypeId: String? = null,
    public val score: Double? = null,
    public val maxRunTime: Double? = null
)
