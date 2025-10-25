package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@EventSerialName("accounts")
public data class Account(
    @Required public val id: String,
    @Required public val username: String,
    public val name: String? = null,
    public val password: String? = null,
    public val type: String? = null,
    public val ip: String? = null,
    public val teamId: String? = null,
    public val personId: String? = null
)
