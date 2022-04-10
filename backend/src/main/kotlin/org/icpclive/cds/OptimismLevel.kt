package org.icpclive.cds

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class OptimismLevel {
    @SerialName("normal")
    NORMAL,
    @SerialName("optimistic")
    OPTIMISTIC,
    @SerialName("pressimistic")
    PESSIMISTIC;

    override fun toString(): String {
        return when (this) {
            NORMAL -> "Normal"
            OPTIMISTIC -> "Optimistic"
            PESSIMISTIC -> "Pessimistic"
        }
    }
}