package org.icpclive.service

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger
import org.icpclive.utils.isHttpUrl
import java.io.IOException
import java.nio.file.Paths
import kotlin.time.Duration

abstract class RegularLoaderService<T>(auth: ClientAuth?) {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processLoaded(data: String): T

    suspend fun loadOnce(): T {
        val content = if (!isHttpUrl(url)) {
            Paths.get(url).toFile().readText()
        } else {
            httpClient.request(url).bodyAsText()
        }
        return processLoaded(content)
    }

    suspend fun run(period: Duration) = flow {
        while (true) {
            try {
                emit(loadOnce())
            } catch (e: IOException) {
                logger.error("Failed to load $url", e)
            }
            delay(period)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        val logger = getLogger(RegularLoaderService::class)
    }
}
