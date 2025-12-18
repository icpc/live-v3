package org.icpclive.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.TypeWithId

abstract class Manager<T> {
    abstract suspend fun add(item: T)
    abstract suspend fun remove(itemId: String)
}

abstract class ManagerWithEvents<T : TypeWithId, E> : Manager<T>() {
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
    protected open fun onItemAdd(item: T) {}
    protected open fun onItemRemove(item: T) {}

    protected suspend fun traverse(block: (T) -> Unit) = mutex.withLock {
        items.forEach(block)
    }

    @IgnorableReturnValue
    private fun removeById(id: String) : Boolean {
        val myItems = items.filter { it.id == id }
        for (item in myItems) {
            onItemRemove(item)
        }
        items.removeAll(myItems)
        return myItems.isNotEmpty()
    }

    override suspend fun add(item: T) = mutex.withLock {
        removeById(item.id) // We don't need the remove event, as create considered as the set on frontend.
        items.add(item)
        onItemAdd(item)
        sendEvent(createAddEvent(item))
    }

    override suspend fun remove(itemId: String) = mutex.withLock {
        if (removeById(itemId)) {
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