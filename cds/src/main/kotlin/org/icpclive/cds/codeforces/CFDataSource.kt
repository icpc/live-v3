package org.icpclive.cds.codeforces

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.FullReloadContestDataSource
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFSubmission
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.cds.common.RegularLoaderService
import org.icpclive.util.getCredentials
import org.icpclive.util.getLogger
import java.io.IOException
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


    private val standingsLoader = object : RegularLoaderService<CFStandings>(null) {
        override val url
            get() = central.standingsUrl

        override fun processLoaded(data: String) = central.parseAndUnwrapStatus(data)
                ?.let { Json.decodeFromJsonElement<CFStandings>(it) }
                ?: throw IOException()
    }

    class CFSubmissionList(val list: List<CFSubmission>)

    private val statusLoader = object : RegularLoaderService<CFSubmissionList>(null) {
        override val url
            get() = central.statusUrl

        override fun processLoaded(data: String) = try {
            central.parseAndUnwrapStatus(data)
                ?.let { Json.decodeFromJsonElement<List<CFSubmission>>(it) }
                ?.let { CFSubmissionList(it) }
                ?: throw IOException()
        } catch (e: SerializationException) {
            throw IOException(e)
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        contestInfo.updateStandings(standingsLoader.loadOnce())
        val runs = contestInfo.parseSubmissions(statusLoader.loadOnce().list)
        return ContestParseResult(contestInfo.toApi(), runs, emptyList())
    }

    companion object {
        private val log = getLogger(CFDataSource::class)
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}
