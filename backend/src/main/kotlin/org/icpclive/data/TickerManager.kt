package org.icpclive.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.icpclive.api.*

object TickerManager {
    private val mutex = Mutex()
    private var timer = 0L
    private val messages = mutableListOf<TickerMessage>()

    suspend fun addMessage(message: TickerMessage) = mutex.withLock {
        messages.add(message)
        timer++
        messagesFlowWrite.emit(timer to AddMessageTickerEvent(message))
    }

    suspend fun removeMessage(id: Long) = mutex.withLock {
        if (messages.removeIf { it.id == id }) {
            timer++
            messagesFlowWrite.emit(timer to RemoveMessageTickerEvent(id))
        }
    }

    suspend fun getMessagesSubscribeEvents() = mutex.withLock {
        timer++
        timer to messages.toList()
    }

    private val messagesFlowWrite = MutableSharedFlow<Pair<Long, TickerEvent>>(
        replay = 32,
        extraBufferCapacity = 100000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val messagesFlow: Flow<TickerEvent> = flow {
        var subscriptionTimer = 0L
        messagesFlowWrite
            .onSubscription {
                val (timer, messages) = getMessagesSubscribeEvents()
                subscriptionTimer = timer
                emit(timer to TickerSnapshotEvent(messages))
            }
            .filter { it.first >= subscriptionTimer }
            .map { it.second }
            .collect { emit(it) }
    }

    init {
        DataBus.tickerFlow.set(messagesFlow)
    }

}
