package org.icpclive.controllers

import kotlinx.serialization.Serializable
import org.icpclive.api.ObjectSettings

@Serializable
data class WidgetState<SettingsType : ObjectSettings>(
    val settings: SettingsType,
    val showOrder: Long? = null,
)
