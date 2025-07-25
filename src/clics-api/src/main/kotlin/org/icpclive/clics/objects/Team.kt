package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@EventSerialName("teams")
public data class Team(
    @Required public val id: String,
    @Required public val name: String,
    public val icpcId: String? = null,
    public val organizationId: String? = null,
    public val groupIds: List<String> = emptyList(),
    public val label: String? = null,
    public val displayName: String? = null,
    public val hidden: Boolean? = null,
    public val photo: List<File> = emptyList(),
    public val video: List<File> = emptyList(),
    public val desktop: List<File> = emptyList(),
    public val webcam: List<File> = emptyList(),
    public val backup: List<File> = emptyList(),
    public val keyLog: List<File> = emptyList(),
    public val toolData: List<File> = emptyList(),
    public val audio: List<File> = emptyList(),
    public val location: TeamLocation? = null
)

@SinceClics(FeedVersion.`2020_03`)
@NoEvent
public class TeamLocation(
    public val x: Double,
    public val y: Double,
    public val rotation: Double
)
