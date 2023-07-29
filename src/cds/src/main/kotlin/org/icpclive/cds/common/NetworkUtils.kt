package org.icpclive.cds.common

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal sealed class ClientAuth {

    class Basic(val login: String, val password: String) : ClientAuth()

    class OAuth(val token: String) : ClientAuth()

    companion object {
        fun BasicOrNull(login: String?, password: String?) = if (login != null && password != null) {
            Basic(login, password)
        } else {
            null
        }
    }
}

internal fun HttpClientConfig<*>.setupAuth(auth: ClientAuth) {
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

private var allowUnsecure = false

fun setAllowUnsecureConnections(value: Boolean) {
    allowUnsecure = value
}

internal fun defaultHttpClient(
    auth: ClientAuth?,
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}
) = HttpClient(CIO) {
    install(HttpTimeout)
    if (auth != null) {
        setupAuth(auth)
    }
    engine {
        threadsCount = 2
        https {
            if (allowUnsecure) {
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


internal fun isHttpUrl(text: String) = text.startsWith("http://") || text.startsWith("https://")
