package org.icpclive.cds.ktor

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.util.getLogger

internal fun getLineFlow(networkSettings: NetworkSettings, url: UrlOrLocalPath): Flow<String> = flow {
    when (url) {
        is UrlOrLocalPath.Local -> {
            url.value.toFile().useLines { lines ->
                emitAll(lines.asFlow())
            }
        }

        is UrlOrLocalPath.Url -> {
            val httpClient = networkSettings.createHttpClient()
            logger.info { "Requesting $url" }
            httpClient.prepareGet(url.value) {
                timeout {
                    socketTimeoutMillis = Long.MAX_VALUE
                    requestTimeoutMillis = Long.MAX_VALUE
                }
                setupAuth(url.auth)
            }.execute { httpResponse ->
                if (httpResponse.status != HttpStatusCode.OK) {
                    logger.warning { "Got ${httpResponse.status} from $url" }
                    return@execute
                }
                val channel = httpResponse.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isEmpty()) continue
                    emit(line)
                }
            }
        }
    }
}.flowOn(Dispatchers.IO)
    .catch { throw wrapIfSSLError(it) }


private val logger by getLogger()