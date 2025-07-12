package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import kotlin.time.Duration

/**
 * This rule allows customization of the queue behavior*
 *
 * All fields can be null, existing values are not changed in that case.
 *
 * @param waitTime Time in the queue for regular run after it was tested.
 * @param firstToSolveWaitTime Time in the queue for first-to-solve run after it was tested.
 * @param featuredRunWaitTime Time in the queue for featured run after it was tested or set featured, whatever is later.
 * @param inProgressRunWaitTime Time in the queue for in-progress run after last progress update.
 * @param maxQueueSize Maximal number of runs in queue.
 * @param maxUntestedRun Maximal number of in-progress runs in queue
 */
@Serializable
@SerialName("overrideQueue")
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
    override fun process(info: ContestInfo): ContestInfo {
        val queueSettings = info.queueSettings
        return info.copy(
            queueSettings = queueSettings.copy(
                waitTime = waitTime ?: queueSettings.waitTime,
                firstToSolveWaitTime = firstToSolveWaitTime ?: queueSettings.firstToSolveWaitTime,
                featuredRunWaitTime = featuredRunWaitTime ?: queueSettings.featuredRunWaitTime,
                inProgressRunWaitTime = inProgressRunWaitTime ?: queueSettings.inProgressRunWaitTime,
                maxQueueSize = maxQueueSize ?: queueSettings.maxQueueSize,
                maxUntestedRun = maxUntestedRun ?: queueSettings.maxUntestedRun,
            )
        )
    }
}