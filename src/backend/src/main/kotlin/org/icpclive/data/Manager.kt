package org.icpclive.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.TypeWithId

abstract class Manager<in T> {
    abstract suspend fun add(item: T)
    abstract suspend fun remove(itemId: String)
}

abstract class ManagerWithEvents<in T : TypeWithId, E> : Manager<T>() {
    private val mutex = Mutex()
    private var timer = 0L
    private val items = mutableListOf<T>()

    private suspend fun sendEvent(e: E) {
        timer++
        flowWrite.emit(timer to e)
    }

    protected abstract fun createAddEvent(item: T): E
    protected abstract fun createRemoveEvent(id: String): E
    protected abstract fun createSnapshotEvent(items: List<T>): E

    override suspend fun add(item: T) = mutex.withLock {
        items.removeIf { it.id == item.id } // We don't need the remove event, as create considered as the set on frontend.
        items.add(item)
        sendEvent(createAddEvent(item))
    }

    override suspend fun remove(itemId: String) = mutex.withLock {
        if (items.removeIf { it.id == itemId }) {
            sendEvent(createRemoveEvent(itemId))
        }
    }

    private suspend fun getSubscribeEvents() = mutex.withLock {
        timer++
        timer to items.toList()
    }

    private val flowWrite = MutableSharedFlow<Pair<Long, E>>(
        replay = 32,
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    protected val flow: Flow<E> = flow {
        var subscriptionTimer = 0L
        flowWrite
            .onSubscription {
                val (timer, widgets) = getSubscribeEvents()
                subscriptionTimer = timer
                emit(timer to createSnapshotEvent(widgets))
            }
            .filter { it.first >= subscriptionTimer }
            .map { it.second }
            .collect { emit(it) }
    }

}