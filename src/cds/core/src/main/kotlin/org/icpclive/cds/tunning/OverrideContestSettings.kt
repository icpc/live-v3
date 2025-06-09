package org.icpclive.cds.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import org.icpclive.cds.util.serializers.HumanTimeSerializer
import kotlin.time.Duration

/**
 * A rule overriding basic settings of contest.
 *
 * All fields can be null, existing values are not changed in that case.
 *
 * If any of [startTime]/[contestLength]/[holdTime] is updated this can change [ContestStatus],
 * based on current time. In particular, overriding [startTime] to the same value can be used
 * to fix contest system reporting start time in the past, but contest in [ContestStatus.BEFORE] state.
 *
 * @param name The name of the contest
 * @param startTime Start time of the contest.
 * @param contestLength Length of contest. Serialized in seconds as `contestLengthSeconds`
 * @param freezeTime Moment of the contest, when the freeze starts. Serialized in seconds as `freezeTimeSeconds`
 * @param holdTime Stopped timer value in before state. Ignored, if resulting contest state is not [ContestStatus.BEFORE].
 */
@Serializable
@SerialName("overrideContestSettings")
public data class OverrideContestSettings(
    public val name: String? = null,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("contestLengthSeconds")
    public val contestLength: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("freezeTimeSeconds")
    public val freezeTime: Duration? = null,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("holdTimeSeconds")
    public val holdTime: Duration? = null,
    public val customFields: Map<String, String?>? = null,
): TuningRule {
    override fun process(info: ContestInfo): ContestInfo {
        val status = ContestStatus.byCurrentTime(
            startTime ?: info.startTime ?: Instant.DISTANT_FUTURE,
            freezeTime ?: info.freezeTime,
            contestLength ?: info.contestLength
        ).let {
            if (it is ContestStatus.BEFORE && holdTime != null) {
                it.copy(holdTime = holdTime)
            } else {
                it
            }
        }
        val realStatus = if (status is ContestStatus.OVER && (info.status is ContestStatus.FINALIZED || info.status is ContestStatus.RUNNING && info.status.isFake)) {
            info.status
        } else {
            status
        }
        if (realStatus != info.status) {
            logger.info { "Contest status is overridden to ${realStatus}, contestLength = ${(contestLength ?: info.contestLength)}" }
        }
        return info.copy(
            name = name ?: info.name,
            status = realStatus,
            contestLength = contestLength ?: info.contestLength,
            freezeTime = freezeTime ?: info.freezeTime,
            customFields = mergeMaps(info.customFields, customFields)
        )
    }

    private companion object {
        val logger by getLogger()
    }
}