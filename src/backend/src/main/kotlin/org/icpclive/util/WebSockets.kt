package org.icpclive.util

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import org.icpclive.cds.util.defaultJsonSettings

suspend fun DefaultWebSocketServerSession.sendFlow(flow: Flow<String>) {
    val sender = async {
        flow.collect {
            val text = Frame.Text(it)
            outgoing.send(text)
        }
    }
    try {
        for (ignored in incoming) {
            ignored.let {}
        }
    } finally {
        sender.cancel()
    }
}

suspend inline fun <reified T> DefaultWebSocketServerSession.sendJsonFlow(flow: Flow<T>) {
    val formatter = defaultJsonSettings()
    sendFlow(flow.map { formatter.encodeToString(it) })
}