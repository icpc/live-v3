package org.icpclive.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.icpclive.api.TeamKeylog
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.cds.api.startTime
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val log by getLogger()

class KeylogService(
    private val httpClient: HttpClient,
    private val cacheTtl: Duration = 15.seconds,
) {
    private data class CacheKey(val teamId: TeamId, val url: String, val intervalMs: Long)
    private data class CachedEntry(val key: CacheKey, val value: TeamKeylog, val expiresAt: TimeMark)

    private val cache = ConcurrentHashMap<TeamId, CachedEntry>()
    private val locks = ConcurrentHashMap<TeamId, Mutex>()

    suspend fun getKeylog(teamId: TeamId): TeamKeylog? {
        val info = DataBus.currentContestInfo()
        val team = info.teams[teamId] ?: return null
        val startTime = info.startTime ?: return null

        val intervalMs = info.keylogSettings.intervalLength.inWholeMilliseconds
        if (intervalMs <= 0) return null

        val keylogUrl = team.medias[TeamMediaType.KEYLOG]
            ?.firstNotNullOfOrNull { (it as? MediaType.Text)?.url }
            ?: return null

        val key = CacheKey(teamId, keylogUrl, intervalMs)
        cache[teamId]?.takeIf { it.key == key && it.expiresAt.hasNotPassedNow() }?.let { return it.value }

        val mutex = locks.computeIfAbsent(teamId) { Mutex() }

        return mutex.withLock {
            cache[teamId]?.takeIf { it.key == key && it.expiresAt.hasNotPassedNow() }?.let { return@withLock it.value }

            val result = fetchAndAggregate(
                url = keylogUrl,
                contestStartMs = startTime.toEpochMilliseconds(),
                contestLengthMs = info.contestLength.inWholeMilliseconds,
                intervalMs = intervalMs,
            )

            cache[teamId] = CachedEntry(key, result, TimeSource.Monotonic.markNow() + cacheTtl)
            result
        }
    }

    private suspend fun fetchAndAggregate(
        url: String,
        contestStartMs: Long,
        contestLengthMs: Long,
        intervalMs: Long,
    ): TeamKeylog {
        val empty = TeamKeylog(intervalMs, emptyList())
        val text = try {
            val response = httpClient.get(url)
            if (!response.status.isSuccess()) {
                log.warning { "Keylog fetch failed at $url: ${response.status}" }
                return empty
            }
            response.bodyAsText()
        } catch (e: Exception) {
            log.warning { "Keylog fetch failed at $url: ${e.message}" }
            return empty
        }

        val values = withContext(Dispatchers.IO) {
            KeylogAggregator.aggregate(
                lines = text.lineSequence(),
                contestStartMs = contestStartMs,
                contestLengthMs = contestLengthMs,
                intervalMs = intervalMs,
            )
        }

        return TeamKeylog(intervalMs, values)
    }
}