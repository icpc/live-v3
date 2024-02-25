package org.icpclive.clics.objects

import org.icpclive.ksp.clics.*
import java.awt.Color

@SinceClics(FeedVersion.`2020_03`)
@UpdateContestEvent
@EventSerialName("problems")
public interface Problem {
    @Required public val id: String
    public val uuid: String?
    @Required public val ordinal: Int
    @Required public val label: String
    @Required public val name: String
    public val rgb: Color?
    public val color: String?
    public val timeLimit: Double?
    public val testDataCount: Int?
    public val maxScore: Double?
    public val statement: List<File>?
}