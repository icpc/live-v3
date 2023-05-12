package org.icpclive.cds.common

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

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

class ByteArrayLoader(
    auth: ClientAuth?,
    val computeURL: () -> String
) : DataLoader<ByteArray> {
    private val httpClient = defaultHttpClient(auth)

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