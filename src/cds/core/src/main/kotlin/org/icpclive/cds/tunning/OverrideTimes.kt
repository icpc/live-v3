package org.icpclive.cds.tunning

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import org.icpclive.cds.util.serializers.HumanTimeSerializer
import kotlin.time.Duration

@Serializable
@SerialName("overrideContestSettings")
public data class OverrideTimes(
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
): TuningRule {
    override fun process(info: ContestInfo, submittedTeams: Set<TeamId>): ContestInfo {
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
        )
    }

    private companion object {
        val logger by getLogger()
    }
}