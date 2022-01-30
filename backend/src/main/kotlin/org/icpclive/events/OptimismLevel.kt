package org.icpclive.events

import org.icpclive.events.OptimismLevel
import java.lang.IllegalArgumentException

enum class OptimismLevel {
    NORMAL, OPTIMISTIC, PESSIMISTIC;

    override fun toString(): String {
        return when (this) {
            NORMAL -> "Normal"
            OPTIMISTIC -> "Optimistic"
            PESSIMISTIC -> "Pessimistic"
            else -> throw IllegalArgumentException()
        }
    }
}