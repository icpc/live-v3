package org.icpclive.cds.ktor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*

internal class HeaderAuthImpl(private val header: String, private val value: String) : ClientAuth {
    override fun setupAuth(config: HttpClientConfig<*>) {
        config.defaultRequest {
            header(header, value)
        }
    }
}

internal class BasicAuthImpl(private val login: String, private val password: String) : ClientAuth {
    override fun setupAuth(config: HttpClientConfig<*>) {
        config.install(Auth) {
            basic {
                credentials { BasicAuthCredentials(this@BasicAuthImpl.login, this@BasicAuthImpl.password) }
                sendWithoutRequest { true }
            }
        }
    }
}


public interface ClientAuth {
    public fun setupAuth(config: HttpClientConfig<*>)

    public companion object {
        public fun basic(login: String, password: String): ClientAuth = BasicAuthImpl(login, password)
        public fun basicOrNull(login: String?, password: String?): ClientAuth? {
            return if (login != null && password != null) {
                basic(login, password)
            } else {
                null
            }
        }
        public fun oauth(token: String) : ClientAuth = HeaderAuthImpl(HttpHeaders.Authorization, "OAuth $token")
        public fun bearer(token: String) : ClientAuth = HeaderAuthImpl(HttpHeaders.Authorization, "Bearer $token")
        public fun cookie(name: String, value: String) : ClientAuth = HeaderAuthImpl(HttpHeaders.Cookie, "${name}=${value}")
        public fun cookieOrNull(name: String?, value: String?) : ClientAuth? =
            if (name != null && value != null) {
                cookie(name, value)
            } else {
                null
            }
    }
}