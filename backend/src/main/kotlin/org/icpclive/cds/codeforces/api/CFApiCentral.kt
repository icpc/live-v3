package org.icpclive.cds.codeforces.api

import org.icpclive.cds.codeforces.api.results.CFStandings
import java.util.Collections
import java.io.IOException
import java.security.NoSuchAlgorithmException
import org.icpclive.cds.codeforces.api.data.CFSubmission
import kotlin.Throws
import java.util.SortedMap
import java.util.TreeMap
import java.lang.StringBuilder
import java.lang.InterruptedException
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.ThreadLocalRandom
import kotlin.jvm.JvmStatic

/**
 * @author egor@egork.net
 */
class CFApiCentral(val contestId: Int) {
    private var apiKey: String? = null
    private var apiSecret: String? = null
    fun setApiKeyAndSecret(apiKey: String?, apiSecret: String?) {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
    }

    private val standingsUrl: String
        get() = apiRequestUrl("contest.standings", mapOf("contestId" to contestId.toString()))
    private val statusUrl: String
        get() = apiRequestUrl("contest.status", mapOf("contestId" to contestId.toString()))

    val standings: CFStandings?
        get() = try {
            Json.decodeFromJsonElement(apiRequest(standingsUrl))
        } catch (e: IOException) {
            log.error("", e)
            null
        }
    val status: List<CFSubmission>?
        get() = try {
            Json.decodeFromJsonElement(apiRequest(statusUrl))
        } catch (e: IOException) {
            log.error("", e)
            null
        }

    private fun apiRequest(urlString: String): JsonElement {
        val url = URL(urlString)
        var node: JsonElement? = null
        while (node == null) {
            try {
                val bytes = url.openConnection().getInputStream().readAllBytes()
                val content = String(bytes, Charsets.UTF_8)
                node = Json.parseToJsonElement(content)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        if (node.jsonObject["status"]!!.jsonPrimitive.content != "OK") {
            throw IOException("Request $urlString unsuccessful")
        }
        return node.jsonObject["result"]!!
    }

    private fun apiRequestUrl(
        method: String,
        params: Map<String, String>
    ): String {
        val sortedParams: SortedMap<String, String> = TreeMap(params)
        val time = System.currentTimeMillis() / 1000
        sortedParams["time"] = time.toString()
        sortedParams["apiKey"] = apiKey
        val rand = (random.nextInt(900000) + 100000).toString()
        val toHash = StringBuilder(rand).append("/").append(method).append("?")
        for ((key, value) in sortedParams) {
            toHash.append(key).append("=").append(value).append("&")
        }
        toHash.deleteCharAt(toHash.length - 1).append("#").append(apiSecret)
        sortedParams["apiSig"] = rand + hash(toHash.toString())
        return buildString {
            append("https://codeforces.com/api/")
            append(method)
            append("?")
            for ((key, value) in sortedParams) {
                append(key)
                append("=")
                append(value)
                append("&")
            }
            deleteCharAt(length - 1)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CFApiCentral::class.java)
        private val random = ThreadLocalRandom.current()
        private fun hash(s: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-512")
            return messageDigest.digest(s.toByteArray(StandardCharsets.UTF_8)).joinToString("") {
                Integer.toHexString(it.toInt() and 0xff or 0x100).substring(1)
            }
        }
    }
}