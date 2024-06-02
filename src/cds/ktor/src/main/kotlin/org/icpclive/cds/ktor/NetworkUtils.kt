package org.icpclive.cds.ktor

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import org.icpclive.cds.settings.*
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@JvmOverloads
public fun defaultHttpClient(
    auth: Auth?,
    networkSettings: NetworkSettings?,
    block: HttpClientConfig<*>.() -> Unit = {},
): HttpClient = HttpClient(CIO) {
    install(HttpTimeout)
    setupAuth(auth)
    engine {
        https {
            if (networkSettings?.allowUnsecureConnections == true) {
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