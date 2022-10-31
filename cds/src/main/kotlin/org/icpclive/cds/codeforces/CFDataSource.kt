package org.icpclive.cds.codeforces

import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.cds.codeforces.api.results.CFStatusWrapper
import org.icpclive.cds.codeforces.api.results.CFSubmissionList
import org.icpclive.cds.common.JsonLoaderService
import org.icpclive.cds.common.map
import org.icpclive.util.getCredentials
import java.lang.IllegalStateException
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CFDataSource(properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val contestInfo = CFContestInfo()
    private val central = CFApiCentral(
        properties.getProperty("contest_id").toInt(),
        properties.getCredentials(CF_API_KEY_PROPERTY_NAME, creds) ?: throw IllegalStateException("No Codeforces api key defined"),
        properties.getCredentials(CF_API_SECRET_PROPERTY_NAME, creds) ?: throw IllegalStateException("No Codeforces api secret defined")
    )


    private val standingsLoader = JsonLoaderService<CFStatusWrapper<CFStandings>>(central.standingsUrl).map {
        it.unwrap()
    }

    private val statusLoader = JsonLoaderService<CFStatusWrapper<CFSubmissionList>>(central.statusUrl).map {
        it.unwrap()
    }

    override suspend fun loadOnce(): ContestParseResult {
        contestInfo.updateStandings(standingsLoader.loadOnce())
        val runs = contestInfo.parseSubmissions(statusLoader.loadOnce().list)
        return ContestParseResult(contestInfo.toApi(), runs, emptyList())
    }

    companion object {
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}
