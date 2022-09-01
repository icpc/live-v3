package org.icpclive.cds.codeforces

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.codeforces.api.CFApiCentral
import org.icpclive.cds.codeforces.api.data.CFSubmission
import org.icpclive.cds.codeforces.api.results.CFStandings
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.RunsBufferService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.*
import java.io.IOException
import java.lang.IllegalStateException
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CFDataSource(properties: Properties) : ContestDataSource {
    private val contestInfo = CFContestInfo()
    private val central = CFApiCentral(
        properties.getProperty("contest_id").toInt(),
        properties.getCredentials(CF_API_KEY_PROPERTY_NAME) ?: throw IllegalStateException("No Codeforces api key defined"),
        properties.getCredentials(CF_API_SECRET_PROPERTY_NAME) ?: throw IllegalStateException("No Codeforces api secret defined")
    )


    private val standingsLoader = object : RegularLoaderService<CFStandings>(null) {
        override val url
            get() = central.standingsUrl

        override fun processLoaded(data: String) = try {
            central.parseAndUnwrapStatus(data)
                ?.let { Json.decodeFromJsonElement<CFStandings>(it) }
                ?: throw IOException()
        } catch (e: SerializationException) {
            throw IOException(e)
        }
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

    override suspend fun run() {
        val contestInfoFlow = MutableStateFlow(contestInfo.toApi())

        coroutineScope {
            val runsBufferFlow = MutableStateFlow<List<RunInfo>>(emptyList())
            val rawRunsFlow = reliableSharedFlow<RunInfo>()
            launch { RunsBufferService(runsBufferFlow, rawRunsFlow).run() }
            launchICPCServices(rawRunsFlow, contestInfoFlow)


            merge(standingsLoader.run(5.seconds), statusLoader.run(5.seconds)).collect {
                when (it) {
                    is CFStandings -> {
                        contestInfo.updateStandings(it)
                        contestInfoFlow.value = contestInfo.toApi()
                    }
                    is CFSubmissionList -> {
                        val submissions = contestInfo.parseSubmissions(it.list)
                        log.info("Loaded ${submissions.size} runs")
                        runsBufferFlow.value = submissions
                    }
                }
            }
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        contestInfo.updateStandings(standingsLoader.loadOnce())
        val runs = contestInfo.parseSubmissions(statusLoader.loadOnce().list)
        return ContestParseResult(contestInfo.toApi(), runs)
    }

    companion object {
        private val log = getLogger(CFDataSource::class)
        private const val CF_API_KEY_PROPERTY_NAME = "cf.api.key"
        private const val CF_API_SECRET_PROPERTY_NAME = "cf.api.secret"
    }
}
