package org.icpclive.service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.NetworkUtils
import org.icpclive.utils.getLogger
import org.icpclive.utils.setupAuth
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import kotlin.time.Duration

abstract class RegularLoaderService<T>(auth: ClientAuth?) {
    private val httpClient = HttpClient {
        if (auth != null) {
            setupAuth(auth)
        }
        engine {
            threadsCount = 2
        }
    }

    abstract val url: String
    abstract fun processLoaded(data: String): T

    suspend fun loadOnce(): T {
        val content = httpClient.request(url).bodyAsText()
        return processLoaded(content)
    }

    suspend fun run(flow: MutableStateFlow<in T>, period: Duration) {
        while (true) {
            try {
                flow.value = loadOnce()
            } catch (e: IOException) {
                logger.error("Failed to load $url", e)
            }
            delay(period)
        }
    }

    companion object {
        val logger = getLogger(RegularLoaderService::class)
    }
}