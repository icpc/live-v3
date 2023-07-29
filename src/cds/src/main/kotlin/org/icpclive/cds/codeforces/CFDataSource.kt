package org.icpclive.cds.codeforces

import kotlinx.datetime.Clock
import org.icpclive.api.ContestStatus
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.common.FullReloadContestDataSource
import org.icpclive.cds.codeforces.api.data.*
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.cds.codeforces.api.results.CFStatusWrapper
import org.icpclive.cds.common.jsonLoader
import org.icpclive.cds.common.map
import org.icpclive.cds.settings.CFSettings
import java.security.MessageDigest
import java.util.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

internal class CFDataSource(val settings: CFSettings, creds: Map<String, String>) : FullReloadContestDataSource(5.seconds) {
    private val contestInfo = CFContestInfo()
    private val apiKey = settings.apiKey.get(creds)
    private val apiSecret = settings.apiSecret.get(creds)

    private fun apiRequestUrl(
        method: String,
        params: Map<String, String>
    ): String {
        val sortedParams = params.toSortedMap()
        sortedParams["time"] = Clock.System.now().epochSeconds.toString()
        sortedParams["apiKey"] = apiKey
        val rand = (Random.nextInt(900000) + 100000).toString()
        sortedParams["apiSig"] = rand + hash(sortedParams.toQuery("$rand/$method?", "#$apiSecret"))
        return sortedParams.toQuery("https://codeforces.com/api/$method?")
    }

    private val standingsLoader = jsonLoader<CFStatusWrapper<CFStandings>> {
        apiRequestUrl(
            "contest.standings",
            mapOf("contestId" to settings.contestId.toString())
        )
    }.map {
        it.unwrap()
    }

    private val statusLoader = jsonLoader<CFStatusWrapper<List<CFSubmission>>> {
        apiRequestUrl(
            "contest.status",
            mapOf("contestId" to settings.contestId.toString())
        )
    }.map {
        it.unwrap()
    }

    private val hacksLoader = jsonLoader<CFStatusWrapper<List<CFHack>>> {
        apiRequestUrl(
            "contest.hacks",
            mapOf("contestId" to settings.contestId.toString())
        )
    }.map {
        it.unwrap()
    }


    override suspend fun loadOnce(): ContestParseResult {
        // can change inside previous if, so we do recheck, not else.
        contestInfo.updateStandings(standingsLoader.load())
        val runs = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseSubmissions(statusLoader.load())
        val hacks = if (contestInfo.status == ContestStatus.BEFORE) emptyList() else contestInfo.parseHacks(hacksLoader.load())
        return ContestParseResult(contestInfo.toApi(), runs + hacks, emptyList())
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        private fun hash(s: String): String =
            MessageDigest.getInstance("SHA-512")
                .digest(s.toByteArray())
                .toHexString()
        private fun SortedMap<String, String>.toQuery(prefix: String, postfix: String="") =
            entries.joinToString(prefix = prefix, postfix = postfix, separator = "&") {
                "${it.key}=${it.value}"
            }
    }
}
