package org.icpclive.cds.common

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.w3c.dom.Document
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.time.Duration

abstract class RegularLoaderService<T>(auth: ClientAuth?) {
    private val httpClient = defaultHttpClient(auth)

    abstract val url: String
    abstract fun processLoaded(data: String): T

    suspend fun loadOnce(): T {
        val content = if (!isHttpUrl(url)) {
            Paths.get(url).toFile().readText()
        } else {
            httpClient.request(url).bodyAsText()
        }
        return processLoaded(content)
    }

    suspend fun run(period: Duration) = flow {
        while (true) {
            emit(loadOnce())
            delay(period)
        }
    }.flowOn(Dispatchers.IO)
}

abstract class XmlLoaderService(auth: ClientAuth?) : RegularLoaderService<Document>(auth) {
    private val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    override fun processLoaded(data: String): Document = builder.parse(data.byteInputStream())
}
