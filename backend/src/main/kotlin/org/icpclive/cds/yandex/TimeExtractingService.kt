package org.icpclive.cds.yandex

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.request
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.cds.yandex.api.SimplifiedFullRunReport

class TimeExtractingService(
    private val httpClient: HttpClient
) {
    // TODO: remove all that code after next Contest release with timeFromStart in Submission

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    private val cache: MutableMap<Long, Long> = mutableMapOf()

    private suspend fun fetchTime(submissionId: Long): Long {
        val response = httpClient.request("submissions/multiple?runIds=$submissionId") {}
        val time = formatter.decodeFromString<List<SimplifiedFullRunReport>>(response.body())[0].timeFromStart
        cache[submissionId] = time
        return time
    }

    suspend fun getTime(submissionId: Long): Long {
        // Can't call computeIfAbsent here: “Suspension functions can be called only within coroutine body” :/
        return cache[submissionId] ?: fetchTime(submissionId)
    }
}