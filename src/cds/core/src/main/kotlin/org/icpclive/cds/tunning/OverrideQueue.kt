package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import kotlin.time.Duration

@Serializable
@SerialName("override_queue")
public data class OverrideQueue(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("waitTimeSeconds")
    public val waitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("firstToSolveWaitTimeSeconds")
    public val firstToSolveWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("featuredRunWaitTimeSeconds")
    public val featuredRunWaitTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("inProgressRunWaitTimeSeconds")
    public val inProgressRunWaitTime: Duration? = null,
    public val maxQueueSize: Int? = null,
    public val maxUntestedRun: Int? = null,
): TuningRule {
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
        return info.copy(
            queueSettings = QueueSettings(
                waitTime = waitTime ?: info.queueSettings.waitTime,
                firstToSolveWaitTime = firstToSolveWaitTime ?: info.queueSettings.firstToSolveWaitTime,
                featuredRunWaitTime = featuredRunWaitTime ?: info.queueSettings.featuredRunWaitTime,
                inProgressRunWaitTime = inProgressRunWaitTime ?: info.queueSettings.inProgressRunWaitTime,
                maxQueueSize = maxQueueSize ?: info.queueSettings.maxQueueSize,
                maxUntestedRun = maxUntestedRun ?: info.queueSettings.maxUntestedRun,
            )
        )
    }
}