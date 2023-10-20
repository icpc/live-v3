package org.icpclive.cds.common

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import org.icpclive.cds.settings.NetworkSettings
import io.ktor.http.*
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

internal sealed class ClientAuth {

    class Basic(val login: String, val password: String) : ClientAuth()

    class OAuth(val token: String) : ClientAuth()

    class Bearer(val token: String) : ClientAuth()

    class CookieAuth(val name: String, val value: String): ClientAuth()

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
                header(HttpHeaders.Authorization, "OAuth ${auth.token}")
            }
        }

        is ClientAuth.Bearer -> {
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer ${auth.token}")
            }
        }

        is ClientAuth.CookieAuth -> {
            defaultRequest {
                header(HttpHeaders.Cookie, "${auth.name}=${auth.value}")
            }
        }
    }
}

internal fun defaultHttpClient(
    auth: ClientAuth?,
    networkSettings: NetworkSettings?,
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}
) = HttpClient(CIO) {
    install(HttpTimeout)
    if (auth != null) {
        setupAuth(auth)
    }
    engine {
        threadsCount = 2
        https {
            if (networkSettings?.allowUnsecureConnections == true) {
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
