package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@EventSerialName("persons")
public data class Person(
    @Required override val id: String,
    public val name: String? = null,
    public val icpcId: String? = null,
    @SingleBefore(FeedVersion.`2023_06`, "team_id") public val teamIds: List<String> = emptyList(),
    public val title: String? = null,
    public val email: String? = null,
    public val sex: String? = null,
    public val role: String? = null,
    public val photo: List<File> = emptyList()
): ObjectWithId
