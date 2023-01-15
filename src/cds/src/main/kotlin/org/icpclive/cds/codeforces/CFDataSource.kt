package org.icpclive.cds.codeforces

import CFHack
import org.icpclive.api.ContestStatus
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFContest
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.cds.codeforces.api.results.CFStatusWrapper
import org.icpclive.cds.codeforces.api.results.CFSubmissionList
import org.icpclive.cds.common.jsonLoaderService
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


    private val standingsLoader = jsonLoaderService<CFStatusWrapper<CFStandings>> { central.standingsUrl }.map {
        it.unwrap()
    }

    private val statusLoader = jsonLoaderService<CFStatusWrapper<CFSubmissionList>> { central.statusUrl }.map {
        it.unwrap()
    }

    private val hacksLoader = jsonLoaderService<CFStatusWrapper<List<CFHack>>> { central.hacksUrl }.map {
        it.unwrap()
    }

    private val contestStatusBeforeLoader = jsonLoaderService<CFStatusWrapper<List<CFContest>>> { central.contestListUrl }.map {
        it.unwrap().single { it.id == contestId }
    }

    override suspend fun loadOnce(): ContestParseResult {
        if (contestInfo.status == ContestStatus.BEFORE) {
            contestInfo.updateContestInfo(contestStatusBeforeLoader.loadOnce())
        }
        // can change inside previous if, so we do recheck, not else.
        if (contestInfo.status != ContestStatus.BEFORE) {
            contestInfo.updateStandings(standingsLoader.loadOnce())
            val runs = contestInfo.parseSubmissions(statusLoader.loadOnce().list)
            val hacks = contestInfo.parseHacks(hacksLoader.loadOnce())
            return ContestParseResult(contestInfo.toApi(), runs + hacks, emptyList())
        } else {
            return ContestParseResult(contestInfo.toApi(), emptyList(), emptyList())
        }
    }

    companion object {
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}
