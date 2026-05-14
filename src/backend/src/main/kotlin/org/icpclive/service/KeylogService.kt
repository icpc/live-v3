package org.icpclive.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.DataLoader
import org.icpclive.cds.ktor.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import kotlin.math.*
import kotlin.time.*

private val log by getLogger()

@Serializable
private data class KeyboardEvent(
    val timestamp: Instant,
    val keys: Map<String, KeyStats> = emptyMap(),
)

@Serializable
private data class KeyStats(
    val bare: Long = 0,
    val shift: Long = 0,
)


class KeylogService(
    private val networkSettings: NetworkSettings,
) {
    suspend fun getKeylog(teamId: TeamId, interval: Duration): List<Long>? {
        val info = DataBus.currentContestInfo()
        val team = info.teams[teamId] ?: return null
        val startTime = info.startTime ?: return null

        val keylogUrl = team.medias[TeamMediaType.KEYLOG]
            ?.firstNotNullOfOrNull { (it as? MediaType.Text)?.url }
            ?: return null

        val lines = DataLoader
            .lineFlow(networkSettings, UrlOrLocalPath.Url(keylogUrl))
            .flowOn(Dispatchers.IO)

        val totalIntervals = ceil(info.contestLength / interval).toInt()
        if (totalIntervals <= 0) {
            return emptyList()
        }

        val buckets = LongArray(totalIntervals)
        lines
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull {
                try {
                    keylogJson.decodeFromString<KeyboardEvent>(it)
                } catch (e: SerializationException) {
                    log.warning { "Skipping malformed keylog line: ${e.message}" }
                    null
                }
            }
            .collect { event ->
                val offset = event.timestamp - startTime
                val index = floor(offset / interval).toInt()
                if (index in buckets.indices) {
                    buckets[index] += event.keys.values.sumOf { it.bare + it.shift }
                }
            }
        return buckets.toList()
    }

    companion object {
        private val keylogJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
