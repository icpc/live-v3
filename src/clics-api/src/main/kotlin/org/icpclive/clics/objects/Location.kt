package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public data class Location(
    public val longitude: Double? = null,
    public val latitude: Double? = null
)
