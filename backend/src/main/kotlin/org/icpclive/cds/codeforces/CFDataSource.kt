package org.icpclive.cds.codeforces

import kotlinx.coroutines.channels.BufferOverflow
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
import org.icpclive.utils.getLogger
import org.icpclive.utils.processCreds
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.seconds

class CFDataSource(properties: Properties) : ContestDataSource {
    private val contestInfo = CFContestInfo()
    private val central = CFApiCentral(properties.getProperty("contest_id").toInt())

    init {
        if (properties.containsKey(CF_API_KEY_PROPERTY_NAME) && properties.containsKey(CF_API_SECRET_PROPERTY_NAME)) {
            central.setApiKeyAndSecret(
                properties.getProperty(CF_API_KEY_PROPERTY_NAME).processCreds(),
                properties.getProperty(CF_API_SECRET_PROPERTY_NAME).processCreds()
            )
        }
    }

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
            val runsBufferFlow = MutableSharedFlow<List<RunInfo>>(
                extraBufferCapacity = 16,
                onBufferOverflow = BufferOverflow.DROP_OLDEST
            )
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
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
                        runsBufferFlow.emit(submissions)
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
