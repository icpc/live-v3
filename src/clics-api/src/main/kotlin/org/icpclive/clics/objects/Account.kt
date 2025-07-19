package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@EventSerialName("accounts")
public data class Account(
    @Required public val id: String,
    @Required public val username: String,
    val name: String? = null,
    public val password: String? = null,
    @Required public val type: String,
    public val ip: String? = null,
    public val teamId: String? = null,
    public val personId: String? = null
)
