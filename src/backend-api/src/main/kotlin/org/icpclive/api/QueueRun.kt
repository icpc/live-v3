package org.icpclive.api

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*

@Serializable
data class QueueRunInfo(
    val id: RunId,
    val result: RunResult,
    val problemId: ProblemId,
    val teamId: TeamId,
    @Required val featuredRunMedia: List<MediaType>? = null,
    @Required val reactionVideos: List<MediaType> = emptyList(),
)