package org.icpclive.cds.ktor

import kotlinx.serialization.Serializable

@Serializable
public data class NetworkSettings(
    public val allowUnsecureConnections: Boolean = false,
    public val checkedServerName: String? = null,
)

public interface KtorNetworkSettingsProvider {
    public val network: NetworkSettings
        get() = NetworkSettings()
}