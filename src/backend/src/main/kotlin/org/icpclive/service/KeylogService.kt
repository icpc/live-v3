package org.icpclive.service

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.TeamId
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.cds.api.startTime
import org.icpclive.cds.util.getLogger
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import kotlin.time.Duration

private val log by getLogger()

class KeylogService(
    private val httpClient: HttpClient,
) {
    suspend fun getKeylog(teamId: TeamId, interval: Duration): List<Long>? {
        val info = DataBus.currentContestInfo()
        val team = info.teams[teamId] ?: return null
        val startTime = info.startTime ?: return null

        val keylogUrl = team.medias[TeamMediaType.KEYLOG]
            ?.firstNotNullOfOrNull { (it as? MediaType.Text)?.url }
            ?: return null

        val response = httpClient.get(keylogUrl)
        if (!response.status.isSuccess()) {
            log.warning { "Keylog fetch failed at $keylogUrl: ${response.status}" }
            return emptyList()
        }
        val text = response.bodyAsText()

        return keylogAggregate(
            lines = text.lineSequence(),
            contestStart = startTime,
            contestLength = info.contestLength,
            interval = interval,
        )
    }
}
