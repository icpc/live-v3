package org.icpclive.cds.codeforces.api

import java.util.SortedMap
import java.util.TreeMap
import java.lang.StringBuilder
import kotlinx.serialization.json.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.random.Random

/**
 * @author egor@egork.net
 */
class CFApiCentral(private val contestId: Int) {
    private var apiKey: String? = null
    private var apiSecret: String? = null
    fun setApiKeyAndSecret(apiKey: String?, apiSecret: String?) {
        this.apiKey = apiKey
        this.apiSecret = apiSecret
    }

    val standingsUrl: String
        get() = apiRequestUrl("contest.standings", mapOf("contestId" to contestId.toString()))
    val statusUrl: String
        get() = apiRequestUrl("contest.status", mapOf("contestId" to contestId.toString()))


    fun parseAndUnwrapStatus(content: String) =
        Json.parseToJsonElement(content)
            .takeIf { it.jsonObject["status"]!!.jsonPrimitive.content == "OK" }
            ?.jsonObject
            ?.get("result")


    private fun apiRequestUrl(
        method: String,
        params: Map<String, String>
    ): String {
        val sortedParams: SortedMap<String, String> = TreeMap(params)
        val time = System.currentTimeMillis() / 1000
        sortedParams["time"] = time.toString()
        sortedParams["apiKey"] = apiKey
        val rand = (Random.nextInt(900000) + 100000).toString()
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
        private fun hash(s: String): String {
            val messageDigest = MessageDigest.getInstance("SHA-512")
            return messageDigest.digest(s.toByteArray(StandardCharsets.UTF_8)).joinToString("") {
                Integer.toHexString(it.toInt() and 0xff or 0x100).substring(1)
            }
        }
    }
}