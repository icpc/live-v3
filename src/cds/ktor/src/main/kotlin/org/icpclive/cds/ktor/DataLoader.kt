package org.icpclive.cds.ktor

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

public fun interface DataLoader<out T> {
    public suspend fun load(): T
}

public inline fun stringLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    crossinline computeURL: () -> UrlOrLocalPath,
) : DataLoader<String> {
    val httpClient = defaultHttpClient(auth, networkSettings)

    return DataLoader {
        when (val url = computeURL()) {
            is UrlOrLocalPath.Local -> url.value.toFile().readText()
            is UrlOrLocalPath.Url -> wrapIfSSLError {
                httpClient.request(url.value).bodyAsText()
            }
        }
    }
}

public inline fun byteArrayLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    crossinline computeURL: () -> UrlOrLocalPath,
): DataLoader<ByteArray> {
    val httpClient = defaultHttpClient(auth, networkSettings)

    return DataLoader {
        when (val url = computeURL()) {
            is UrlOrLocalPath.Local -> url.value.toFile().readBytes()
            is UrlOrLocalPath.Url -> wrapIfSSLError {
                httpClient.request(url.value).body<ByteArray>()
            }
        }
    }
}


public inline fun xmlLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth?,
    crossinline url: () -> UrlOrLocalPath,
): DataLoader<Document> {
    val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    return stringLoader(networkSettings, auth, url)
        .map { builder.parse(it.byteInputStream()) }
}

public inline fun <reified T> jsonLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    crossinline url: () -> UrlOrLocalPath,
): DataLoader<T> {
    val json = Json { ignoreUnknownKeys = true }
    return stringLoader(networkSettings, auth, url)
        .map { json.decodeFromString(it) }
}

public inline fun <reified T> jsonUrlLoader(
    networkSettings: NetworkSettings?,
    auth: ClientAuth? = null,
    crossinline url: () -> String,
): DataLoader<T> = jsonLoader<T>(networkSettings, auth) { UrlOrLocalPath.Url(url()) }


public inline fun <T, R> DataLoader<T>.map(crossinline f: suspend (T) -> R): DataLoader<R> = DataLoader { f(this@map.load()) }