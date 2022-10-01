package org.icpclive.api

import kotlinx.serialization.Serializable
import org.icpclive.utils.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
data class RunInfo constructor(
    val id: Int,
    val isAccepted: Boolean,
    val isJudged: Boolean,
    val isAddingPenalty: Boolean,
    val result: String,
    val problemId: Int,
    val teamId: Int,
    val percentage: Double,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val isFirstSolvedRun: Boolean = false,
    val featuredRunMedia: TeamMediaType? = null,
    val isHidden: Boolean = false,
)
