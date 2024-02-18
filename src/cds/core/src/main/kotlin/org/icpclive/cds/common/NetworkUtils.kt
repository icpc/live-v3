package org.icpclive.cds.common

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.icpclive.cds.settings.NetworkSettings
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

public sealed class ClientAuth {

    public class Basic(public val login: String, public val password: String) : ClientAuth()

    public class OAuth(public val token: String) : ClientAuth()

    public class Bearer(public val token: String) : ClientAuth()

    public class CookieAuth(public val name: String, public val value: String) : ClientAuth()

    public companion object {
        public fun BasicOrNull(login: String?, password: String?): Basic? = if (login != null && password != null) {
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

public fun defaultHttpClient(
    auth: ClientAuth?,
    networkSettings: NetworkSettings?,
    block: HttpClientConfig<CIOEngineConfig>.() -> Unit = {},
): HttpClient = HttpClient(CIO) {
    install(HttpTimeout)
    if (auth != null) {
        setupAuth(auth)
    }
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
    }
    block()
}
