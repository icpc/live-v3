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

object NetworkUtils {
    private val log = getLogger(NetworkUtils::class)
    fun prepareNetwork(login: String?, password: String?) {
        if (login == null || password == null) return
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? {
                    return null
                }

                override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
            }
        )
        try {
            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: NoSuchAlgorithmException) {
            log.error("error", e)
        } catch (e: KeyManagementException) {
            log.error("error", e)
        }


        // Create all-trusting host name verifier
        val allHostsValid = HostnameVerifier { _, _ -> true }
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
        Authenticator.setDefault(
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(login, password.toCharArray())
                }
            })
        CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))

//        System.setProperty("javax.net.ssl.keyStore", "C:/work/icpc-live/resources/key.jks");
//        System.setProperty("javax.net.ssl.trustStore", "C:/work/icpc-live/resources/key.jks");
    }

    fun openAuthorizedStream(url: String, auth: ClientAuth?): InputStream {
        if (!url.contains("http")) {
            return FileInputStream(url)
        }
        CookieHandler.setDefault(CookieManager(null, CookiePolicy.ACCEPT_ALL))
        val con = URL(url).openConnection() as HttpURLConnection
        auth?.run {
            con.setRequestProperty("Authorization", auth.header)
        }
        con.connect()
        for ((key, value) in con.headerFields) {
            log.debug("$key=$value")
        }
        return con.inputStream
    }

}