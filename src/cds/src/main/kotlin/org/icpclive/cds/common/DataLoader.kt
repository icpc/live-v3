package org.icpclive.cds.common

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.util.intervalFlow
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration

interface DataLoader<out T> {
    suspend fun load(): T
}

class StringLoader(
    auth: ClientAuth?,
    val computeURL: () -> String
) : DataLoader<String> {
    private val httpClient = defaultHttpClient(auth)

    override suspend fun load(): String {
        val url = computeURL()
        val content = if (!isHttpUrl(url)) {
            Paths.get(url).toFile().readText()
        } else {
            httpClient.request(url).bodyAsText()
        }
        return content
    }
}

fun xmlLoader(auth: ClientAuth? = null, url: () -> String): DataLoader<Document> {
    val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return StringLoader(auth, url)
        .map { builder.parse(it.byteInputStream()) }
}


inline fun <reified T> jsonLoader(auth: ClientAuth? = null, noinline url: () -> String) : DataLoader<T> {
    val json = Json { ignoreUnknownKeys = true }
    return StringLoader(auth, url)
        .map { json.decodeFromString(it) }
}

fun <T, R> DataLoader<T>.map(f: suspend (T) -> R) = object : DataLoader<R> {
    override suspend fun load() = f(this@map.load())
}

fun <T> DataLoader<T>.reloadFlow(interval: Duration) = intervalFlow(interval).map { load() }.flowOn(Dispatchers.IO)
