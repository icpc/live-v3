package org.icpclive.cds.common

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration

interface LoaderService<out T> {
    suspend fun loadOnce(): T
    suspend fun run(period: Duration): Flow<T>
}

private abstract class LoaderServiceImpl<out T>(
    val computeURL: () -> String,
    auth: ClientAuth?
) : LoaderService<T> {
    private val httpClient = defaultHttpClient(auth)

    abstract fun processLoaded(data: String): T

    override suspend fun loadOnce(): T {
        val url = computeURL()
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

private class XmlLoaderService(url: () -> String, auth: ClientAuth?) : LoaderServiceImpl<Document>(url, auth) {
    private val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    override fun processLoaded(data: String): Document = builder.parse(data.byteInputStream())
}

private class StringLoaderService(url: () -> String, auth: ClientAuth?) : LoaderServiceImpl<String>(url, auth) {
    override fun processLoaded(data: String) = data
}

fun xmlLoaderService(auth: ClientAuth? = null, url: () -> String): LoaderService<Document> {
    return XmlLoaderService(url, auth)
}

fun stringLoaderService(auth: ClientAuth? = null, url: () -> String): LoaderService<String> {
    return StringLoaderService(url, auth)
}

inline fun <reified T> jsonLoaderService(auth: ClientAuth? = null, noinline url: () -> String) : LoaderService<T> {
    val json = Json { ignoreUnknownKeys = true }
    return stringLoaderService(auth, url).map {
        json.decodeFromString(it)
    }
}

fun <T, R> LoaderService<T>.map(f: suspend (T) -> R) = object : LoaderService<R> {
    val delegate = this@map
    override suspend fun loadOnce() = f(delegate.loadOnce())
    override suspend fun run(period: Duration) = delegate.run(period).map(f)
}
