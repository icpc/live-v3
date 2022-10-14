package org.icpclive.service

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import org.icpclive.utils.*
import java.nio.file.Paths
import kotlin.time.Duration.Companion.seconds

abstract class LineStreamLoaderService<T>(auth: ClientAuth?) {
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
    }.flowOn(Dispatchers.IO)

    companion object {
        val logger = getLogger(LineStreamLoaderService::class)
    }
}
