package org.icpclive.cds.codeforces

import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.cds.codeforces.api.results.CFStatusWrapper
import org.icpclive.cds.codeforces.api.results.CFSubmissionList
import org.icpclive.cds.common.jsonLoader
import org.icpclive.cds.common.map
import org.icpclive.util.getCredentials
import java.lang.IllegalStateException
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CFDataSource(properties: Properties, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val contestInfo = CFContestInfo()
    private val contestId = properties.getProperty("contest_id").toInt()
    private val central = CFApiCentral(
        contestId,
        properties.getCredentials(CF_API_KEY_PROPERTY_NAME, creds) ?: throw IllegalStateException("No Codeforces api key defined"),
        properties.getCredentials(CF_API_SECRET_PROPERTY_NAME, creds) ?: throw IllegalStateException("No Codeforces api secret defined")
    )


    private val standingsLoader = jsonLoader<CFStatusWrapper<CFStandings>> { central.standingsUrl }.map {
        it.unwrap()
    }

    private val statusLoader = jsonLoader<CFStatusWrapper<CFSubmissionList>> { central.statusUrl }.map {
        it.unwrap()
    }

    private val hacksLoader = jsonLoader<CFStatusWrapper<List<CFHack>>> { central.hacksUrl }.map {
        it.unwrap()
    }


    override suspend fun loadOnce(): ContestParseResult {
        // can change inside previous if, so we do recheck, not else.
        contestInfo.updateStandings(standingsLoader.load())
        val runs = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseSubmissions(statusLoader.load().list)
        val hacks = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseHacks(hacksLoader.load())
        return ContestParseResult(contestInfo.toApi(), runs + hacks, emptyList())
    }

    companion object {
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}
