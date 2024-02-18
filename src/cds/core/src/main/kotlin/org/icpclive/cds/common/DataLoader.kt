package org.icpclive.cds.common

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.util.wrapIfSSLError
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

public interface DataLoader<out T> {
    public suspend fun load(): T
}

@PublishedApi
internal class StringLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    val computeURL: () -> UrlOrLocalPath,
) : DataLoader<String> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

    override suspend fun load(): String {
        val content = when (val url = computeURL()) {
            is UrlOrLocalPath.Local -> url.value.toFile().readText()
            is UrlOrLocalPath.Url -> wrapIfSSLError {
                httpClient.request(url.value).bodyAsText()
            }
        }
        return content
    }
}

internal class ByteArrayLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    val computeURL: () -> UrlOrLocalPath,
) : DataLoader<ByteArray> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

    override suspend fun load(): ByteArray {
        val content = when (val url = computeURL()) {
            is UrlOrLocalPath.Local -> url.value.toFile().readBytes()
            is UrlOrLocalPath.Url -> wrapIfSSLError {
                httpClient.request(url.value).body<ByteArray>()
            }
        }
        return content
    }
}


public fun byteArrayLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    url: () -> UrlOrLocalPath,
): DataLoader<ByteArray> {
    return ByteArrayLoader(networkSettings, auth, url)
}


public fun xmlLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    url: () -> UrlOrLocalPath,
): DataLoader<Document> {
    val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return StringLoader(networkSettings, auth, url)
        .map { builder.parse(it.byteInputStream()) }
}


public inline fun <reified T> jsonLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    noinline url: () -> UrlOrLocalPath,
): DataLoader<T> {
    val json = Json { ignoreUnknownKeys = true }
    return StringLoader(networkSettings, auth, url)
        .map { json.decodeFromString(it) }
}

public inline fun <reified T> jsonUrlLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    noinline url: () -> String,
): DataLoader<T> = jsonLoader<T>(networkSettings, auth) { UrlOrLocalPath.Url(url()) }


public fun <T, R> DataLoader<T>.map(f: suspend (T) -> R): DataLoader<R> = object : DataLoader<R> {
    override suspend fun load() = f(this@map.load())
}