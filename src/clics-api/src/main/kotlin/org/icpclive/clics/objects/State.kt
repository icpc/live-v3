package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("state")
public data class State(
    public val started: Instant? = null,
    public val frozen: Instant? = null,
    public val ended: Instant? = null,
    public val unfrozen: Instant? = null,
    public val thawed: Instant? = null,
    public val finalized: Instant? = null,
    public val endOfUpdates: Instant? = null
)
