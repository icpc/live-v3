package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public interface Endpoint {
    public val type: String?
    public val properties: List<String>
}