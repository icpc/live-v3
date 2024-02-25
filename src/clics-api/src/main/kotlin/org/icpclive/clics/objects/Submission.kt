package org.icpclive.clics.objects

import kotlinx.datetime.Instant
import org.icpclive.ksp.clics.*
import kotlin.time.Duration

@SinceClics(FeedVersion.`2020_03`)
@UpdateRunEvent
@EventSerialName("submissions")
public interface Submission {
    @Required public val id: String
    public val languageId: String?
    @Required public val teamId: String
    @Required public val problemId: String
    public val time: Instant?
    @Required public val contestTime: Duration
    public val entryPoint: String?
    public val files: List<File>
    public val reaction: List<File>?
}