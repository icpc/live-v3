package org.icpclive.cds.settings

import kotlinx.serialization.Serializable
import org.icpclive.cds.util.serializers.HumanTimeSerializer
import kotlin.time.Instant

@Serializable
public class EmulationSettings(
    public val speed: Double,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant,
    public val useRandomInProgress: Boolean = true,
)