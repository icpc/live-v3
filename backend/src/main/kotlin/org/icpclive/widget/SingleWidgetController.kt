package org.icpclive.widget

import org.icpclive.api.ObjectSettings
import org.icpclive.api.ObjectStatus
import org.icpclive.api.TypeWithId

interface SingleWidgetController<SettingsType : ObjectSettings, OverlayWidgetType : TypeWithId> {
    suspend fun getStatus(): ObjectStatus<SettingsType>
    suspend fun setSettings(newSettings: SettingsType)
    suspend fun show()
    suspend fun show(newSettings: SettingsType)
    suspend fun hide()
}
