package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public interface Access {
    public val capabilities: List<String>
    public val endpoints: List<Endpoint>
}

