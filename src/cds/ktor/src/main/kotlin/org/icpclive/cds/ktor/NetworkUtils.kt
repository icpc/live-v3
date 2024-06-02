package org.icpclive.cds.ktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.icpclive.cds.settings.*
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

public fun NetworkSettings.createHttpClient(): HttpClient = createHttpClient {}

public fun NetworkSettings.createHttpClient(block: HttpClientConfig<*>.() -> Unit, ): HttpClient =
    HttpClient(CIO) {
        install(HttpTimeout)
        engine {
            https {
                if (allowUnsecureConnections) {
                    trustManager = object : X509TrustManager {
                        override fun getAcceptedIssuers() = null
                        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String?) {}
                        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String?) {}
                    }
                }
            }
            requestTimeout = 40000
        }
        block()
    }