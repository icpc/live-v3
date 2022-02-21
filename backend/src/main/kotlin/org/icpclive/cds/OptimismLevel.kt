package org.icpclive.cds

enum class OptimismLevel {
    NORMAL, OPTIMISTIC, PESSIMISTIC;

    override fun toString(): String {
        return when (this) {
            NORMAL -> "Normal"
            OPTIMISTIC -> "Optimistic"
            PESSIMISTIC -> "Pessimistic"
        }
    }
}