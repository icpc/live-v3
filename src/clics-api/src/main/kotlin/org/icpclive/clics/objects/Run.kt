package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@UpdateRunEvent
@EventSerialName("runs")
public interface Run {
    @Required public val id: String
    @Required public val judgementId: String
    @Required public val time: Instant
    public val ordinal: Int?
    public val judgementTypeId: String?
    public val contestTime: Duration?
    public val runtime: Double?
}