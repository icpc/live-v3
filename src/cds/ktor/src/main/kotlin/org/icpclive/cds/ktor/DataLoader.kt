package org.icpclive.cds.ktor

import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.util.getLogger
import org.w3c.dom.Document
import java.io.File
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

public fun interface DataLoader<out T> {
    public suspend fun load(): T

    public companion object {
        private val logger by getLogger()

        private fun <T> impl(
            networkSettings: NetworkSettings?,
            auth: ClientAuth?,
            computeURL: () -> UrlOrLocalPath,
            processFile: File.() -> T,
            processRequest: suspend HttpResponse.() -> T
        ) : DataLoader<T> {
            val httpClient = defaultHttpClient(auth, networkSettings)

            return DataLoader {
                when (val url = computeURL()) {
                    is UrlOrLocalPath.Local -> url.value.toFile().processFile()
                    is UrlOrLocalPath.Url -> wrapIfSSLError {
                        logger.debug { "Requesting ${url.value}" }
                        httpClient.request(url.value).processRequest()
                    }
                }
            }
        }

        public fun string(
            networkSettings: NetworkSettings?,
            auth: ClientAuth?,
            computeURL: () -> UrlOrLocalPath,
        ): DataLoader<String> = impl(networkSettings, auth, computeURL, File::readText, HttpResponse::bodyAsText)

        public fun byteArray(
            networkSettings: NetworkSettings?,
            auth: ClientAuth?,
            computeURL: () -> UrlOrLocalPath,
        ): DataLoader<ByteArray> = impl(networkSettings, auth, computeURL, File::readBytes, { body<ByteArray>() })

        public fun xml(
            networkSettings: NetworkSettings?,
            auth: ClientAuth?,
            url: () -> UrlOrLocalPath,
        ): DataLoader<Document> {
            val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            return string(networkSettings, auth, url)
                .map { builder.parse(it.byteInputStream()) }
        }

        public fun <T> json(
            serializer: DeserializationStrategy<T>,
            networkSettings: NetworkSettings?,
            auth: ClientAuth? = null,
            url: () -> UrlOrLocalPath,
        ): DataLoader<T> {
            val json = Json { ignoreUnknownKeys = true }
            return string(networkSettings, auth, url)
                .map { json.decodeFromString(serializer, it) }
        }
        public inline fun <reified T> json(
            networkSettings: NetworkSettings?,
            auth: ClientAuth? = null,
            noinline url: () -> UrlOrLocalPath,
        ): DataLoader<T> = json(serializer<T>(), networkSettings, auth, url)

        public inline fun <reified T> json(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: UrlOrLocalPath, ): DataLoader<T> =
            json(serializer<T>(), networkSettings, auth) { url }
        public fun string(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: UrlOrLocalPath): DataLoader<String> =
            string(networkSettings, auth) { url }
        public fun byteArray(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: UrlOrLocalPath): DataLoader<ByteArray> =
            byteArray(networkSettings, auth) { url }
        public fun xml(networkSettings: NetworkSettings?, auth: ClientAuth? = null, url: UrlOrLocalPath): DataLoader<Document> =
            xml(networkSettings, auth) { url }
        public fun lineFlow(networkSettings: NetworkSettings?, auth: ClientAuth?, url: UrlOrLocalPath) : Flow<String> =
            getLineFlow(networkSettings, auth, url)
    }
}

public inline fun <T, R> DataLoader<T>.map(crossinline f: suspend (T) -> R): DataLoader<R> = DataLoader { f(this@map.load()) }