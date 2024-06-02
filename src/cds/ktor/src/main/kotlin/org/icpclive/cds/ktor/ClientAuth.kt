package org.icpclive.cds.ktor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.icpclive.cds.settings.*
import io.ktor.client.plugins.auth.Auth as AuthPlugin

internal class HeaderAuthImpl(private val header: String, private val value: String) : ClientAuth {
    override fun setupAuth(config: HttpClientConfig<*>) {
        config.defaultRequest {
            header(header, value)
        }
    }
}

internal class BasicAuthImpl(private val login: String, private val password: String) : ClientAuth {
    override fun setupAuth(config: HttpClientConfig<*>) {
        config.install(AuthPlugin) {
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

public fun HttpRequestBuilder.setupAuth(auth: Auth?) {
    if (auth == null) return
    auth.basic?.let { basicAuth(it.login.value, it.password.value) }
    for ((name, value) in auth.cookies) {
        cookie(name, value.value)
    }
    for ((name, value) in auth.headers) {
        header(name, value)
    }
}

public fun Auth.withOAuth(token: Credential): Auth = withHeader(HttpHeaders.Authorization, Credential("OAuth ${token.value}", "OAuth ${token.displayValue}"))
public fun Auth.withBearer(token: Credential): Auth = withHeader(HttpHeaders.Authorization, Credential("Bearer ${token.value}", "Bearer ${token.displayValue}"))

public fun UrlOrLocalPath.Url.withOAuth(token: Credential): UrlOrLocalPath.Url = withHeader(HttpHeaders.Authorization, Credential("OAuth ${token.value}", "OAuth ${token.displayValue}"))
public fun UrlOrLocalPath.Url.withBearer(token: Credential): UrlOrLocalPath.Url = withHeader(HttpHeaders.Authorization, Credential("Bearer ${token.value}", "Bearer ${token.displayValue}"))

