package org.icpclive.service

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.icpclive.cds.util.getLogger
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Instant

private val log by getLogger()

private val keylogJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
internal data class KeyboardEvent(
    val timestamp: Instant,
    val keys: Map<String, KeyStats> = emptyMap(),
)

@Serializable
internal data class KeyStats(
    val bare: Int? = null,
    val shift: Int? = null,
)

fun keylogAggregate(
    lines: Sequence<String>,
    contestStart: Instant,
    contestLength: Duration,
    interval: Duration,
): List<Long> {
    val totalIntervals = ceil(contestLength / interval).toInt()
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

        val offset = event.timestamp - contestStart
        if (offset < Duration.ZERO) {
            continue
        }
        val index = (offset / interval).toInt()
        if (index >= totalIntervals) {
            continue
        }

        buckets[index] += event.keys.values.sumOf { (it.bare ?: 0).toLong() + (it.shift ?: 0).toLong() }
    }

    return buckets.toList()
}
