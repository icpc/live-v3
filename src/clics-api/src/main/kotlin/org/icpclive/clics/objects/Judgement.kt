package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("judgements")
public interface Judgement {
    @Required public val id: String
    @Required public val submissionId: String
    @Required public val startContestTime: Duration
    @Required public val startTime: Instant
    public val endTime: Instant?
    public val endContestTime: Duration?
    public val judgementTypeId: String?
    public val score: Double?
    public val maxRunTime: Double?
}