package org.icpclive.cds.settings

import kotlinx.serialization.Serializable

@Serializable
public class NetworkSettings(
    public val allowUnsecureConnections: Boolean = false,
)