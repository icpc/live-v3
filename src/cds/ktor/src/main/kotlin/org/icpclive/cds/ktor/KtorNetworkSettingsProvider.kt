package org.icpclive.cds.ktor

import kotlinx.serialization.Serializable

@Serializable
public class NetworkSettings(
    public val allowUnsecureConnections: Boolean = false,
)

public interface KtorNetworkSettingsProvider {
    public val network: NetworkSettings
        get() = NetworkSettings()
}