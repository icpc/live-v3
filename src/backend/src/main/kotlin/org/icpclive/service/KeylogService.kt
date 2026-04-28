package org.icpclive.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.icpclive.api.TeamKeylog
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.cds.api.startTime
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo

private val log by getLogger()

class KeylogService(
    private val httpClient: HttpClient,
) {
    suspend fun getKeylog(teamId: TeamId): TeamKeylog? {
        val info = DataBus.currentContestInfo()
        val team = info.teams[teamId] ?: return null
        val startTime = info.startTime ?: return null

        val intervalMs = info.keylogSettings.intervalLength.inWholeMilliseconds
        if (intervalMs <= 0) return null

        val keylogUrl = team.medias[TeamMediaType.KEYLOG]
            ?.firstNotNullOfOrNull { (it as? MediaType.Text)?.url }
            ?: return null

        val empty = TeamKeylog(intervalMs, emptyList())
        val text = try {
            val response = httpClient.get(keylogUrl)
            if (!response.status.isSuccess()) {
                log.warning { "Keylog fetch failed at $keylogUrl: ${response.status}" }
                return empty
            }
            response.bodyAsText()
        } catch (e: Exception) {
            log.warning { "Keylog fetch failed at $keylogUrl: ${e.message}" }
            return empty
        }

        val values = withContext(Dispatchers.IO) {
            KeylogAggregator.aggregate(
                lines = text.lineSequence(),
                contestStartMs = startTime.toEpochMilliseconds(),
                contestLengthMs = info.contestLength.inWholeMilliseconds,
                intervalMs = intervalMs,
            )
        }

        return TeamKeylog(intervalMs, values)
    }
}
