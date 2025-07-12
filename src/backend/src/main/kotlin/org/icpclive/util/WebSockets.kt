package org.icpclive.util

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.server.serverResponseJsonSettings

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
    val formatter = serverResponseJsonSettings()
    sendFlow(flow.map { formatter.encodeToString(it) })
}