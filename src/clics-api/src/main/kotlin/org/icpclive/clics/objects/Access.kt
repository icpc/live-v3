package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@NoEvent
public data class Access(
    public val capabilities: List<String>,
    public val endpoints: List<Endpoint>,
)

