package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("state")
public interface State {
    public val started: Instant?
    public val frozen: Instant?
    public val ended: Instant?
    public val unfrozen: Instant?
    public val thawed: Instant?
    public val finalized: Instant?
    public val endOfUpdates: Instant?
}