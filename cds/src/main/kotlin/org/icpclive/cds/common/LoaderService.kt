package org.icpclive.cds.common

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration

interface LoaderService<out T> {
    suspend fun loadOnce(): T
    suspend fun run(period: Duration): Flow<T>
}

abstract class LoaderServiceImpl<out T>(auth: ClientAuth?) : LoaderService<T> {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processLoaded(data: String): T

    override suspend fun loadOnce(): T {
        val content = if (!isHttpUrl(url)) {
            Paths.get(url).toFile().readText()
        } else {
            httpClient.request(url).bodyAsText()
        }
        return processLoaded(content)
    }

    override suspend fun run(period: Duration) = flow {
        while (true) {
            emit(loadOnce())
            delay(period)
        }
    }.flowOn(Dispatchers.IO)
}

class XmlLoaderService(override val url: String, auth: ClientAuth? = null) : LoaderServiceImpl<Document>(auth) {
    private val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    override fun processLoaded(data: String): Document = builder.parse(data.byteInputStream())
}

class JsonLoaderService<out T>(
    override val url: String,
    val serializer: KSerializer<out T>,
    auth: ClientAuth?
) : LoaderServiceImpl<T>(auth) {
    val json = Json { ignoreUnknownKeys = true }
    override fun processLoaded(data: String): T = json.decodeFromString(serializer, data)
}

inline fun <reified T> JsonLoaderService(url: String, auth: ClientAuth? = null) = JsonLoaderService<T>(url, serializer(), auth)

fun <T, R> LoaderService<T>.map(f: suspend (T) -> R) = object : LoaderService<R> {
    val delegate = this@map
    override suspend fun loadOnce() = f(delegate.loadOnce())
    override suspend fun run(period: Duration) = delegate.run(period).map(f)
}
