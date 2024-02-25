package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("teams")
public interface Team {
    @Required public val id: String
    @Required public val name: String
    public val organizationId: String?
    public val groupIds: List<String>
    public val label: String?
    public val displayName: String?
    public val hidden: Boolean?
    public val photo: List<File>
    public val video: List<File>
    public val desktop: List<File>
    public val webcam: List<File>
    public val backup: List<File>
    public val key_log: List<File>
    public val tool_data: List<File>
}