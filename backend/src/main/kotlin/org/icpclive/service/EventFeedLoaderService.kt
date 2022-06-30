package org.icpclive.service

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger
import org.icpclive.utils.setupAuth

abstract class EventFeedLoaderService<T>(private val auth: ClientAuth?) {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processEvent(data: String): T?


    suspend fun run(flow: MutableSharedFlow<T>) {
        while (true) {
            httpClient.prepareGet(url) {
                timeout {
                    socketTimeoutMillis = Long.MAX_VALUE
                    requestTimeoutMillis = Long.MAX_VALUE
                }
            }.execute { httpResponse ->
                val channel = httpResponse.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isEmpty()) continue
                    processEvent(line)?.also { flow.emit(it) }
                }
            }
            println("Reconnect")
        }
    }

    companion object {
        val logger = getLogger(EventFeedLoaderService::class)
    }
}
