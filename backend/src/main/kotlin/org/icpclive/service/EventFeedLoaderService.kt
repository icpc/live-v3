package org.icpclive.service

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger

abstract class EventFeedLoaderService<T>(auth: ClientAuth?) {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processEvent(data: String): T?


    suspend fun run() = flow {
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
                    processEvent(line)?.also { emit(it) }
                }
            }
            println("Reconnect")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        val logger = getLogger(EventFeedLoaderService::class)
    }
}
