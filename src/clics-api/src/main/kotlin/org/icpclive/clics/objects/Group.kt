package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("groups")
public data class Group(
    @Required override val id: String,
    public val icpcId: String? = null,
    public val name: String? = null,
    public val type: String? = null,
    public val location: Location? = null
) : ObjectWithId
