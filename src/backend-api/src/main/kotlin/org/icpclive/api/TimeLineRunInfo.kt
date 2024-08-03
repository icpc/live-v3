package org.icpclive.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ProblemId
import org.icpclive.cds.util.serializers.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
sealed class TimeLineRunInfo {
    @Serializable
    @SerialName("ICPC")
    public data class ICPC(
        @Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration,
        val problemId: ProblemId,
        val isAccepted: Boolean,
        val shortName: String) : TimeLineRunInfo()

    @Serializable
    @SerialName("IOI")
    public data class IOI(@Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration, val problemId: ProblemId, val score: Double) : TimeLineRunInfo()

    @Serializable
    @SerialName("IN_PROGRESS")
    public data class InProgress(@Serializable(with = DurationInMillisecondsSerializer::class) val time: Duration, val problemId: ProblemId) : TimeLineRunInfo()
}
