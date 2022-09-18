package org.icpclive.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import org.icpclive.config
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.X509TrustManager

sealed class ClientAuth {

    class Basic(val login: String, val password: String) : ClientAuth()

    class OAuth(val token: String) : ClientAuth()
}

fun HttpClientConfig<*>.setupAuth(auth: ClientAuth) {
    when (auth) {
        is ClientAuth.Basic -> {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials(auth.login, auth.password) }
                    sendWithoutRequest { true }
                }
            }
        }
        is ClientAuth.OAuth -> {
            defaultRequest {
                header("Authorization", "OAuth ${auth.token}")
            }
        }
    }
}

fun defaultHttpClient(auth: ClientAuth?, block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}) = HttpClient(CIO) {
    install(HttpTimeout)
    if (auth != null) {
        setupAuth(auth)
    }
    engine {
        threadsCount = 2
        https {
            if (config.allowUnsecureConnections) {
                trustManager = object : X509TrustManager {
                    override fun getAcceptedIssuers() = null
                    override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String?) {}
                    override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String?) {}
                }
            }
        }
    }
    block()
}


fun isHttpUrl(text: String) = text.startsWith("http://") || text.startsWith("https://")
