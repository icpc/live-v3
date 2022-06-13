package org.icpclive.utils

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import java.io.FileInputStream
import java.io.InputStream
import java.net.*
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.*

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