package org.icpclive.cds.ktor

import java.io.IOException
import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

internal class LiveSSLException(message: String, cause: Throwable?) : IOException(message, cause)

@PublishedApi
internal fun wrapIfSSLError(e: Throwable): Throwable = if (e is SSLException || e is GeneralSecurityException) {
    LiveSSLException("There are some https related errors. If you don't care, add \"network\": {\"allowUnsecureConnections\": true} to your config.", e)
} else e

@PublishedApi
internal inline fun <T> wrapIfSSLError(block: () -> T): T = try {
    block()
} catch (e: Throwable) {
    throw wrapIfSSLError(e)
}