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
import java.io.IOException
import java.nio.file.Paths
import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

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
    .catch { throw wrapIfSSLError(it) }

internal class LiveSSLException(message: String, cause: Throwable?) : IOException(message, cause)

internal fun wrapIfSSLError(e: Throwable) = if (e is SSLException || e is GeneralSecurityException) {
        LiveSSLException("There are some https related errors. If you don't care, add \"network\": {\"allowUnsecureConnections\": true} to your config.", e)
    } else e

private object LineStreamLoaderService
private val logger = getLogger(LineStreamLoaderService::class)