package org.icpclive.utils

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import org.icpclive.config.Config
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.X509TrustManager

sealed class ClientAuth {
    abstract val header: String
}

class BasicAuth(val login: String, val password: String) : ClientAuth() {
    override val header = "Basic " + Base64.getEncoder().encodeToString("$login:$password".toByteArray())
}

class OAuthAuth(val token: String) : ClientAuth() {
    override val header = "OAuth $token"
}

fun HttpClientConfig<*>.setupAuth(auth: ClientAuth) {
    when (auth) {
        is BasicAuth -> {
            install(Auth) {
                basic {
                    credentials { BasicAuthCredentials(auth.login, auth.password) }
                }
            }
        }
        is OAuthAuth -> {
            defaultRequest {
                header("Authorization", "OAuth ${auth.token}")
            }
        }
    }
}

fun defaultHttpClient(auth: ClientAuth?, block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}) = HttpClient(CIO) {
    if (auth != null) {
        setupAuth(auth)
    }
    engine {
        threadsCount = 2
        https {
            if (Config.allowUnsecureConnections) {
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