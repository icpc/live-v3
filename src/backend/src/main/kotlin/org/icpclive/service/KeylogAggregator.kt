package org.icpclive.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.icpclive.cds.util.getLogger
import java.time.DateTimeException
import kotlin.time.Duration
import kotlin.time.Instant

private val log by getLogger()

private val keylogJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
internal data class KeyboardEvent(
    val timestamp: String,
    val keys: Map<String, KeyStats> = emptyMap(),
)

@Serializable
internal data class KeyStats(
    val bare: Int? = null,
    val shift: Int? = null,
)

internal object KeylogAggregator {
    fun aggregate(
        lines: Sequence<String>,
        contestStartMs: Long,
        contestLengthMs: Long,
        intervalMs: Long,
    ): List<Double> {
        val totalIntervals = ((contestLengthMs + intervalMs - 1) / intervalMs).toInt()
        if (totalIntervals <= 0) {
            return emptyList()
        }

        val buckets = LongArray(totalIntervals)
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) {
                continue
            }

            val event = try {
                keylogJson.decodeFromString(KeyboardEvent.serializer(), line)
            } catch (e: Exception) {
                log.warning { "Skipping malformed keylog line: ${e.message}" }
                continue
            }

            val eventMs = try {
                Instant.parse(event.timestamp).toEpochMilliseconds()
            } catch (e: DateTimeException) {
                log.warning { "Skipping malformed keylog timestamp: ${event.timestamp}" }
                continue
            }

            if (eventMs < contestStartMs) {
                continue
            }
            val index = ((eventMs - contestStartMs) / intervalMs).toInt()
            if (index >= totalIntervals) {
                continue
            }

            var presses = 0L
            for (key in event.keys.values) {
                presses += (key.bare ?: 0).toLong() + (key.shift ?: 0).toLong()
            }
            buckets[index] += presses
        }

        val perMinute = 60_000.0 / intervalMs
        return List(totalIntervals) { buckets[it] * perMinute }
    }
}