package org.icpclive.cds.settings

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.util.HumanTimeSerializer

@Serializable
public class EmulationSettings(
    public val speed: Double,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant
)