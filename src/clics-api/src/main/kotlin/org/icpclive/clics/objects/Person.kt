package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*

@SinceClics(FeedVersion.`2022_07`)
@UpdateContestEvent
@EventSerialName("persons")
public interface Person {
    @Required public val id: String
    public val name: String?
    public val icpcId: String?
    public val teamId: String?
    public val title: String?
    public val email: String?
    public val sex: String?
    public val role: String?
    public val photo: List<File>
}