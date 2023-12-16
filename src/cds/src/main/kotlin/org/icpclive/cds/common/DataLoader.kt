package org.icpclive.cds.common

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
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
    val computeURL: () -> UrlOrLocalPath
) : DataLoader<String> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

    override suspend fun load(): String {
        val url = computeURL()
        val content = when (url) {
            is UrlOrLocalPath.Local -> url.value.toFile().readText()
            is UrlOrLocalPath.Url -> try {
                httpClient.request(url.value).bodyAsText()
            } catch (e: Throwable) {
                throw wrapIfSSLError(e)
            }
        }
        return content
    }
}

internal class ByteArrayLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    val computeURL: () -> UrlOrLocalPath
) : DataLoader<ByteArray> {
    private val httpClient = defaultHttpClient(auth, networkSettings)

    override suspend fun load(): ByteArray {
        val url = computeURL()
        val content = when (url) {
            is UrlOrLocalPath.Local -> url.value.toFile().readBytes()
            is UrlOrLocalPath.Url -> try {
                httpClient.request(url.value).body<ByteArray>()
            } catch (e: Throwable) {
                throw wrapIfSSLError(e)
            }
        }
        return content
    }
}


internal fun xmlLoader(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: () -> UrlOrLocalPath): DataLoader<Document> {
    val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return StringLoader(networkSettings, auth, url)
        .map { builder.parse(it.byteInputStream()) }
}


internal inline fun <reified T> jsonLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    noinline url: () -> UrlOrLocalPath
) : DataLoader<T> {
    val json = Json { ignoreUnknownKeys = true }
    return StringLoader(networkSettings, auth, url)
        .map { json.decodeFromString(it) }
}

internal inline fun <reified T> jsonUrlLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    noinline url: () -> String
) = jsonLoader<T>(networkSettings, auth) { UrlOrLocalPath.Url(url())}


internal fun <T, R> DataLoader<T>.map(f: suspend (T) -> R) = object : DataLoader<R> {
    override suspend fun load() = f(this@map.load())
}