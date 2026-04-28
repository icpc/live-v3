package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.util.serializers.DurationInSecondsSerializer
import kotlin.time.Duration

@Serializable
@SerialName("overrideKeylog")
public data class OverrideKeylog(
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("intervalSeconds")
    public val intervalLength: Duration? = null,
) : TuningRule {
    override fun process(info: ContestInfo): ContestInfo {
        val current = info.keylogSettings
        return info.copy(
            keylogSettings = current.copy(
                intervalLength = intervalLength ?: current.intervalLength
            )
        )
    }
}