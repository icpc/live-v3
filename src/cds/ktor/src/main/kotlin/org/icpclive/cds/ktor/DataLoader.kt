package org.icpclive.cds.ktor

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.logger
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

public fun interface DataLoader<out T> {
    public suspend fun load(): T

    public companion object {
        private val logger by getLogger()

        private fun <T> impl(
            networkSettings: NetworkSettings,
            computeURL: () -> UrlOrLocalPath,
            processFile: File.() -> T,
            processRequest: suspend HttpResponse.() -> T
        ) : DataLoader<T> {
            val httpClient = networkSettings.createHttpClient()

            return DataLoader {
                when (val url = computeURL()) {
                    is UrlOrLocalPath.Local -> url.value.toFile().processFile()
                    is UrlOrLocalPath.Url -> wrapIfSSLError {
                        logger.debug { "Requesting ${url.value}" }
                        httpClient.request(url.value) {
                            setupAuth(url.auth)
                        }.processRequest()
                    }
                }
            }
        }

        public fun string(
            networkSettings: NetworkSettings,
            computeURL: () -> UrlOrLocalPath,
        ): DataLoader<String> = impl(networkSettings, computeURL, File::readText, HttpResponse::bodyAsText)

        public fun byteArray(
            networkSettings: NetworkSettings,
            computeURL: () -> UrlOrLocalPath,
        ): DataLoader<ByteArray> = impl(networkSettings, computeURL, File::readBytes) { body<ByteArray>() }

        public fun xml(
            networkSettings: NetworkSettings,
            url: () -> UrlOrLocalPath,
        ): DataLoader<Document> {
            val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            return string(networkSettings, url)
                .map { builder.parse(it.byteInputStream()) }
        }

        public fun <T> json(
            serializer: DeserializationStrategy<T>,
            networkSettings: NetworkSettings,
            url: () -> UrlOrLocalPath,
        ): DataLoader<T> {
            val json = Json { ignoreUnknownKeys = true }
            return string(networkSettings, url)
                .map { json.decodeFromString(serializer, it) }
        }
        public inline fun <reified T> json(
            networkSettings: NetworkSettings,
            noinline url: () -> UrlOrLocalPath,
        ): DataLoader<T> = json(serializer<T>(), networkSettings, url)

        public inline fun <reified T> json(
            networkSettings: NetworkSettings,
            url: UrlOrLocalPath
        ): DataLoader<T> = json(serializer<T>(), networkSettings) { url }
        public fun string(
            networkSettings: NetworkSettings,
            url: UrlOrLocalPath
        ): DataLoader<String> = string(networkSettings) { url }
        public fun byteArray(
            networkSettings: NetworkSettings,
            url: UrlOrLocalPath
        ): DataLoader<ByteArray> = byteArray(networkSettings) { url }
        public fun xml(
            networkSettings: NetworkSettings,
            url: UrlOrLocalPath
        ): DataLoader<Document> = xml(networkSettings) { url }
        public fun lineFlow(
            networkSettings: NetworkSettings,
            url: UrlOrLocalPath
        ) : Flow<String> = getLineFlow(networkSettings, url)
    }
}

public inline fun <T, R> DataLoader<T>.map(crossinline f: suspend (T) -> R): DataLoader<R> = DataLoader { f(this@map.load()) }
public fun <T:Any> DataLoader<T>.cached(interval: Duration) = object : DataLoader<T> {
    var nextLoad: TimeMark? = null
    lateinit var cache: T
    override suspend fun load(): T {
        if (nextLoad?.hasPassedNow() != false) {
            cache = this@cached.load()
            nextLoad = TimeSource.Monotonic.markNow() + interval
        } else {
            logger(DataLoader::class.java).info {
                "Not loading because of cache"
            }
        }
        return cache
    }
}