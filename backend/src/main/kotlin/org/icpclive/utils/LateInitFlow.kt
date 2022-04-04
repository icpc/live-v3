package org.icpclive.utils

import kotlinx.coroutines.flow.*

class LateInitFlow<T> {
    private val flow = MutableStateFlow<Flow<T>?>(null)
    fun set(x: Flow<T>) { flow.update { if (it == null) x else throw IllegalStateException("Can be set only once") } }
    suspend fun get() = flow.filterNotNull().first()
}