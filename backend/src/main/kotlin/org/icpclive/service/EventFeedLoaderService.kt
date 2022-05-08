package org.icpclive.service

import io.ktor.client.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.NetworkUtils
import org.icpclive.utils.getLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.time.Duration

abstract class EventFeedLoaderService<T>(private val auth: ClientAuth?) {
    private val httpClient = HttpClient {
//        if (auth != null) {
//            setupAuth(auth)
//        }
        engine {
            threadsCount = 2
        }
    }

    abstract val url: String
    abstract fun processEvent(data: String): T?

    private fun buildReader() =
        BufferedReader(InputStreamReader(NetworkUtils.openAuthorizedStream(url, auth), StandardCharsets.UTF_8))

    suspend fun loadOnce(flow: MutableSharedFlow<T>) {
        val reader = buildReader()
        while (true) {
            val eventString = withContext(Dispatchers.IO) { reader.readLine() }
            eventString?.let { processEvent(it)?.let { e -> flow.emit(e) } } ?: break
        }
    }

    suspend fun run(flow: MutableSharedFlow<T>, period: Duration) {
        while (true) {
            try {

            } catch (e: IOException) {
                logger.error("Failed to load events from $url", e)
            }
            delay(period)
        }
    }

    companion object {
        val logger = getLogger(EventFeedLoaderService::class)
    }
}
