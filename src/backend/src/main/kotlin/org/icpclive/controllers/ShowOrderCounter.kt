package org.icpclive.controllers

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.concurrent.atomics.update

class ShowOrderCounter {
    private val last = AtomicLong(0)

    fun next(): Long = last.incrementAndFetch()

    fun observe(showOrder: Long) {
        last.update { maxOf(it, showOrder) }
    }
}
