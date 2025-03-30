package org.icpclive.cds.ktor

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.icpclive.cds.settings.*
import io.ktor.client.plugins.auth.Auth as AuthPlugin

public fun HttpMessageBuilder.setupHeaders(auth: Authorization) {
    for ((name, value) in auth.cookies) {
        cookie(name, decodeCookieValue(value.value, CookieEncoding.URI_ENCODING))
    }
    for ((name, value) in auth.headers) {
        header(name, value)
    }
}


public fun HttpClientConfig<*>.setupAuth(auth: Authorization?) {
    if (auth == null) return
    auth.basic?.let {
        install(AuthPlugin) {
            basic {
                credentials { BasicAuthCredentials(it.login.value, it.password.value) }
                sendWithoutRequest { true }
            }
        }
    }
    defaultRequest {
        setupHeaders(auth)
    }
}


public fun HttpRequestBuilder.setupAuth(auth: Authorization?) {
    if (auth == null) return
    auth.basic?.let { basicAuth(it.login.value, it.password.value) }
    setupHeaders(auth)
}

public fun Authorization.withOAuth(token: Credential?): Authorization = if (token == null) this else withHeader(HttpHeaders.Authorization, Credential("OAuth ${token.value}", "OAuth ${token.displayValue}"))
public fun Authorization.withBearer(token: Credential?): Authorization = if (token == null) this else withHeader(HttpHeaders.Authorization, Credential("Bearer ${token.value}", "Bearer ${token.displayValue}"))

public fun UrlOrLocalPath.Url.withOAuth(token: Credential?): UrlOrLocalPath.Url = if (token == null) this else withHeader(HttpHeaders.Authorization, Credential("OAuth ${token.value}", "OAuth ${token.displayValue}"))
public fun UrlOrLocalPath.Url.withBearer(token: Credential?): UrlOrLocalPath.Url = if (token == null) this else withHeader(HttpHeaders.Authorization, Credential("Bearer ${token.value}", "Bearer ${token.displayValue}"))

