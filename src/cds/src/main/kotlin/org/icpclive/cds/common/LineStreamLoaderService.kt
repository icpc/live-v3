package org.icpclive.cds.common

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.util.getLogger
import java.nio.file.Paths

internal fun getLineStreamLoaderFlow(networkSettings: NetworkSettings?, auth: ClientAuth?, url: UrlOrLocalPath) = flow {
    when (url) {
        is UrlOrLocalPath.Local -> {
            url.value.toFile().useLines { lines ->
                emitAll(lines.asFlow())
            }
        }

        is UrlOrLocalPath.Url -> {
            val httpClient = defaultHttpClient(auth, networkSettings)
            logger.debug("Requesting $url")
            httpClient.prepareGet(url.value) {
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
                    emit(line)
                }
            }
        }
    }
}.flowOn(Dispatchers.IO)

private object LineStreamLoaderService
private val logger = getLogger(LineStreamLoaderService::class)