package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public interface Location {
    public val longitude: Double?
    public val latitude: Double?
}