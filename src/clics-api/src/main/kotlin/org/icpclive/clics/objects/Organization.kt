package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("organizations")
public data class Organization(
    @Required public val id: String,
    public val icpcId: String? = null,
    public val name: String? = null,
    public val formalName: String? = null,
    public val country: String? = null,
    public val countryFlag: List<File>? = null,
    public val url: String? = null,
    public val twitterHashtag: String? = null,
    @SinceClics(FeedVersion.`2023_06`) public val twitterAccount: String? = null,
    public val location: Location? = null,
    public val logo: List<File>? = null
)
