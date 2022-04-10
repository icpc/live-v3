package org.icpclive.cds

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OptimismLevel {
    @SerialName("normal")
    NORMAL,
    @SerialName("optimistic")
    OPTIMISTIC,
    @SerialName("pessimistic")
    PESSIMISTIC;

    override fun toString(): String {
        return when (this) {
            NORMAL -> "Normal"
            OPTIMISTIC -> "Optimistic"
            PESSIMISTIC -> "Pessimistic"
        }
    }
}
