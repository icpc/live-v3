package org.icpclive.cds.common

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.NetworkSettings
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

internal interface DataLoader<out T> {
    suspend fun load(): T
}

internal class StringLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    val computeURL: () -> String
) : DataLoader<String> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

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

internal class ByteArrayLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    val computeURL: () -> String
) : DataLoader<ByteArray> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

    override suspend fun load(): ByteArray {
        val url = computeURL()
        val content = if (!isHttpUrl(url)) {
            Paths.get(url).toFile().readBytes()
        } else {
            httpClient.request(url).body<ByteArray>()
        }
        return content
    }
}


internal fun xmlLoader(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: () -> String): DataLoader<Document> {
    val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return StringLoader(networkSettings, auth, url)
        .map { builder.parse(it.byteInputStream()) }
}


internal inline fun <reified T> jsonLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    noinline url: () -> String
) : DataLoader<T> {
    val json = Json { ignoreUnknownKeys = true }
    return StringLoader(networkSettings, auth, url)
        .map { json.decodeFromString(it) }
}

internal fun <T, R> DataLoader<T>.map(f: suspend (T) -> R) = object : DataLoader<R> {
    override suspend fun load() = f(this@map.load())
}