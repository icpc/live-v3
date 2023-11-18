package org.icpclive.util

import java.io.IOException
import java.security.GeneralSecurityException
import javax.net.ssl.SSLException

class LiveSSLException(message: String, cause: Throwable?) : IOException(message, cause)

fun wrapIfSSLError(e: Throwable) = if (e is SSLException || e is GeneralSecurityException) {
    LiveSSLException("There are some https related errors. If you don't care, add \"network\": {\"allowUnsecureConnections\": true} to your config.", e)
} else e

inline fun <T> wrapIfSSLError(block: () -> T) = try {
    block()
} catch (e: Throwable) {
    throw wrapIfSSLError(e)
}