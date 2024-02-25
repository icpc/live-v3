package org.icpclive.clics.v202003.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class Operation {
    @SerialName("create")
    CREATE,

    @SerialName("update")
    UPDATE,

    @SerialName("delete")
    DELETE
}
