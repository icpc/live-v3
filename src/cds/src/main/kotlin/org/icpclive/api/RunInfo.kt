package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
data class RunInfo constructor(
    val id: Int,
    val result: RunResult?,
    val percentage: Double,
    val problemId: Int,
    val teamId: Int,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val time: Duration,
    val featuredRunMedia: MediaType? = null,
    val reactionVideos: List<MediaType> = emptyList(),
    val isHidden: Boolean = false,
)

@Serializable
sealed class RunResult

@Serializable
@SerialName("icpc")
data class ICPCRunResult(
    val isAccepted: Boolean,
    val isAddingPenalty: Boolean,
    val isFirstToSolveRun: Boolean,
    val result: String,
) : RunResult()

@Serializable
@SerialName("ioi")
data class IOIRunResult(
    val score: List<Double>,
    val wrongVerdict: String? = null,
    val difference: Double = 0.0,
    val isFirstBestRun: Boolean = false
) : RunResult()