package org.icpclive.cds.codeforces.api

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random

/**
 * @author egor@egork.net
 */
class CFApiCentral(
    private val contestId: Int,
    private val apiKey: String,
    private val apiSecret: String,) {

    val standingsUrl: String
        get() = apiRequestUrl("contest.standings", mapOf("contestId" to contestId.toString()))
    val statusUrl: String
        get() = apiRequestUrl("contest.status", mapOf("contestId" to contestId.toString()))


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