package org.icpclive.api

import kotlinx.serialization.Serializable


interface TypeWithId {
    val id: String
}

@Serializable
data class ObjectStatus<SettingsType : ObjectSettings>(
    val shown: Boolean,
    val settings: SettingsType,
    val id: Int?
)