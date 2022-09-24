package org.icpclive.service

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger
import org.icpclive.utils.isHttpUrl
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

abstract class EventFeedLoaderService<T>(auth: ClientAuth?) {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processEvent(data: String): T?


    suspend fun run() = flow {
        if (!isHttpUrl(url)) {
            Paths.get(url).toFile().useLines { lines ->
                lines.forEach { processEvent(it)?.also { emit(it) } }
            }
            return@flow
        }

        while (true) {
            logger.warn("Requesting $url")
            httpClient.prepareGet(url) {
                timeout {
                    socketTimeoutMillis = Long.MAX_VALUE
                    requestTimeoutMillis = Long.MAX_VALUE
                }
            }.execute { httpResponse ->
                if (httpResponse.status != HttpStatusCode.OK) {
                    logger.warn("Got ${httpResponse.status} from $url")
                    return@execute
                }
                val channel = httpResponse.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isEmpty()) continue
                    processEvent(line)?.also { emit(it) }
                }
            }
            delay(5.seconds)
            logger.warn("Reconnect")
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        val logger = getLogger(EventFeedLoaderService::class)
    }
}
