package org.icpclive.cds.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.AnalyticsCommentaryEvent
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.clics.api.Event
import org.icpclive.cds.clics.api.Event.*
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.common.LineStreamLoaderService
import org.icpclive.util.*
import java.util.*
import kotlin.time.Duration.Companion.seconds

enum class FeedVersion {
    V2020_03,
    V2022_07
}

private class ClicsLoaderSettings(properties: Properties, prefix: String, creds: Map<String, String>) {
    private val url = properties.getProperty("${prefix}url")

    val auth = ClientAuth.BasicOrNull(
        properties.getCredentials("${prefix}login", creds),
        properties.getCredentials("${prefix}password", creds)
    )
    val eventFeedUrl = apiRequestUrl(properties.getProperty("${prefix}event_feed_name", "event-feed"))

    private fun apiRequestUrl(method: String) = "$url/$method"

    val feedVersion = FeedVersion.valueOf("V" + properties.getProperty("${prefix}feed_version", "2022_07"))
}

class ClicsDataSource(properties: Properties, creds: Map<String, String>) : ContestDataSource {
    private val mainLoaderSettings = ClicsLoaderSettings(properties, "", creds)
    private val additionalLoaderSettings = properties.getProperty("additional_feed.url", null)?.let {
        ClicsLoaderSettings(properties, "additional_feed.", creds)
    }

    private val model = ClicsModel(
        properties.getProperty("use_team_names", "true") == "true",
        properties.getProperty("hidden_groups", "").split(",").toSet(),
        properties.getProperty("media_base_url", "")
    )

    val Event.isFinalEvent get() = this is StateEvent && data?.end_of_updates != null

    fun CoroutineScope.launchLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (AnalyticsCommentaryEvent) -> Unit
    ) {
        val eventsLoader = getEventFeedLoader(mainLoaderSettings)
        val additionalEventsLoader = additionalLoaderSettings?.let { getEventFeedLoader(it, true) }

        launch {
            fun priority(event: UpdateContestEvent) = when (event) {
                is ContestEvent -> 0
                is StateEvent -> 1
                is JudgementTypeEvent -> 2
                is OrganizationEvent -> 3
                is GroupsEvent -> 4
                is TeamEvent -> 5
                is ProblemEvent -> 6
                is PreloadFinishedEvent -> throw IllegalStateException()
            }

            fun priority(event: UpdateRunEvent) = when (event) {
                is SubmissionEvent -> 0
                is JudgementEvent -> 1
                is RunsEvent -> 2
            }

            fun Flow<Event>.sortedPrefix() = flow {
                val channelDeferred = CompletableDeferred<ReceiveChannel<Event>>()
                launch {
                    @OptIn(FlowPreview::class)
                    produceIn(this).also { channelDeferred.completeOrThrow(it) }
                }
                val channel = channelDeferred.await()
                val prefix = mutableListOf<Event>()
                prefix.add(channel.receive())
                while (true) {
                    try {
                        withTimeout(1.seconds) {
                            channel.receiveCatching().getOrNull()
                        }?.let { prefix.add(it) } ?: break
                    } catch (e: TimeoutCancellationException) {
                        break
                    }
                }
                val contestEvents = prefix.filterIsInstance<UpdateContestEvent>()
                val runEvents = prefix.filterIsInstance<UpdateRunEvent>()
                val otherEvents = prefix.filter { it !is UpdateContestEvent && it !is UpdateRunEvent }
                contestEvents.sortedBy { priority(it) }.forEach { emit(it) }
                runEvents.sortedBy { priority(it) }.forEach { emit(it) }
                otherEvents.forEach { emit(it) }
                emit(PreloadFinishedEvent(""))
                if (contestEvents.none { it.isFinalEvent }) {
                    for (event in channel) {
                        emit(event)
                        if (event.isFinalEvent) break
                    }
                }
                channel.cancel()
            }

            var preloadFinished = false

            suspend fun processEvent(it: Event) {
                when (it) {
                    is UpdateContestEvent -> {
                        val changedRuns = when (it) {
                            is ContestEvent -> model.processContest(it.data!!)
                            is ProblemEvent -> model.processProblem(it.id, it.data)
                            is OrganizationEvent -> model.processOrganization(it.id, it.data)
                            is TeamEvent -> model.processTeam(it.id, it.data)
                            is StateEvent -> model.processState(it.data!!)
                            is JudgementTypeEvent -> model.processJudgementType(it.id, it.data)
                            is GroupsEvent -> model.processGroup(it.id, it.data)
                            is PreloadFinishedEvent -> {
                                preloadFinished = true
                                model.getAllRuns()
                            }
                        }
                        if (preloadFinished) {
                            onContestInfo(model.contestInfo)
                            for (run in changedRuns) {
                                onRun(run)
                            }
                        }
                    }

                    is UpdateRunEvent -> {
                        when (it) {
                            is SubmissionEvent -> model.processSubmission(it.data!!)
                            is JudgementEvent -> model.processJudgement(it.data!!)
                            is RunsEvent -> model.processRun(it.data!!)
                        }.also { run ->
                            if (preloadFinished) {
                                onRun(run.toApi())
                            }
                        }
                    }

                    is CommentaryEvent -> {
                        val data = it.data
                        if (data != null) {
                            onComment(
                                AnalyticsCommentaryEvent(
                                    data.id,
                                    data.message,
                                    data.time,
                                    data.contest_time,
                                    data.team_ids?.map { model.liveTeamId(it) } ?: emptyList(),
                                    data.submission_ids?.map { model.liveSubmissionId(it) } ?: emptyList(),
                                )
                            )
                        }
                    }

                    is IgnoredEvent -> {}
                }
            }

            val idSet = mutableSetOf<String>()
            merge(eventsLoader.run(), additionalEventsLoader?.run() ?: emptyFlow())
                .sortedPrefix()
                .filterNot { it.token in idSet }
                .onEach { processEvent(it) }
                .onEach { idSet.add(it.token) }
                .logAndRetryWithDelay(5.seconds) {
                    logger.error("Exception caught in CLICS parser. Will restart in 5 seconds.", it)
                    preloadFinished = false
                }.collect()
        }
    }

    override suspend fun run(
        contestInfoDeferred: CompletableDeferred<StateFlow<ContestInfo>>,
        runsDeferred: CompletableDeferred<Flow<RunInfo>>,
        analyticsMessagesDeferred: CompletableDeferred<Flow<AnalyticsMessage>>
    ) {
        coroutineScope {
            val contestInfoFlow = MutableStateFlow(model.contestInfo)
            val rawRunsFlow = reliableSharedFlow<RunInfo>()
            val analyticsEventsFlow = reliableSharedFlow<AnalyticsMessage>()
            launchLoader(
                onRun = { rawRunsFlow.emit(it) },
                onContestInfo = { contestInfoFlow.value = it },
                onComment = { analyticsEventsFlow.emit(it) }
            )
            runsDeferred.completeOrThrow(rawRunsFlow)
            contestInfoDeferred.completeOrThrow(contestInfoFlow)
            analyticsMessagesDeferred.completeOrThrow(analyticsEventsFlow)
        }
    }

    override suspend fun loadOnce(): ContestParseResult {
        val analyticsMessages = mutableListOf<AnalyticsMessage>()
        coroutineScope {
            launchLoader(
                onRun = {},
                onContestInfo = {},
                onComment = { analyticsMessages.add(it) }
            )
        }
        logger.info("Loaded data from CLICS")
        val runs = model.getAllRuns()
        return ContestParseResult(model.contestInfo, runs, analyticsMessages)
    }

    companion object {
        val logger = getLogger(ClicsDataSource::class)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private fun getEventFeedLoader(settings: ClicsLoaderSettings, verbose: Boolean = false) =
    object : LineStreamLoaderService<Event>(settings.auth) {
        private val jsonDecoder = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        override val url = settings.eventFeedUrl
        override fun processEvent(data: String) = try {
            if (verbose) {
                logger.info("Got event: $data")
            }
            when (settings.feedVersion) {
                FeedVersion.V2020_03 -> Event.fromV1(jsonDecoder.decodeFromString(data))
                FeedVersion.V2022_07 -> jsonDecoder.decodeFromString(data)
            }
        } catch (e: SerializationException) {
            logger.error("Failed to deserialize: $data", e)
            null
        }
    }
