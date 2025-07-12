package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import kotlin.time.Duration
import kotlin.time.Instant

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("runs")
public data class Run(
    @Required public val id: String,
    @Required public val judgementId: String,
    @Required public val time: Instant,
    public val ordinal: Int? = null,
    public val judgementTypeId: String? = null,
    public val contestTime: Duration? = null,
    public val runtime: Double? = null
)
