package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@EventSerialName("accounts")
public interface Account {
    @Required public val id: String
    @Required public val username: String
    public val password: String?
    @Required public val type: String
    public val ip: String?
    public val teamId: String?
    public val personId: String?
}