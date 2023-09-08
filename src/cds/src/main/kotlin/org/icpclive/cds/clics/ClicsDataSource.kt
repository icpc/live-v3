package org.icpclive.cds.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.icpclive.api.*
import org.icpclive.cds.*
import org.icpclive.clics.Event
import org.icpclive.clics.Event.*
import org.icpclive.cds.common.*
import org.icpclive.cds.settings.*
import org.icpclive.util.getLogger
import org.icpclive.util.logAndRetryWithDelay
import kotlin.time.Duration.Companion.seconds

private class ParsedClicsLoaderSettings(settings: ClicsLoaderSettings, creds: Map<String, String>) {
    private val url = settings.url

    val auth = ClientAuth.BasicOrNull(
        settings.login?.get(creds),
        settings.password?.get(creds)
    )
    val eventFeedUrl = apiRequestUrl(settings.eventFeedName)

    private fun apiRequestUrl(method: String) = "$url/$method"

    val feedVersion = settings.feedVersion
}

internal class ClicsDataSource(val settings: ClicsSettings, creds: Map<String, String>) : ContestDataSource {
    private val mainLoaderSettings = ParsedClicsLoaderSettings(settings.mainFeed, creds)
    private val additionalLoaderSettings = settings.additionalFeed?.let { ParsedClicsLoaderSettings(it, creds) }

    private val model = ClicsModel(
        settings.useTeamNames,
        settings.mediaBaseUrl
    )

    val Event.isFinalEvent get() = this is StateEvent && data?.end_of_updates != null

    suspend fun runLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (AnalyticsCommentaryEvent) -> Unit
    ) {
        val eventsLoader = getEventFeedLoader(mainLoaderSettings, settings.network)
        val additionalEventsLoader = additionalLoaderSettings?.let { getEventFeedLoader(it, settings.network) }

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
            coroutineScope {
                val channel = produceIn(this)
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
                val contestEvents = prefix.filterIsInstance<UpdateContestEvent>().sortedBy { priority(it) }
                val runEvents = prefix.filterIsInstance<UpdateRunEvent>().sortedBy { priority(it) }
                val otherEvents = prefix.filter { it !is UpdateContestEvent && it !is UpdateRunEvent }
                contestEvents.filter { !it.isFinalEvent }.forEach { emit(it) }
                runEvents.forEach { emit(it) }
                otherEvents.forEach { emit(it) }
                emit(PreloadFinishedEvent(""))
                contestEvents.filter { it.isFinalEvent }.forEach { emit(it) }
                for (event in channel) {
                    emit(event)
                }
            }
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
                    it.data?.let { comment ->
                        onComment(
                            model.processCommentary(comment)
                        )
                    }
                }

                is IgnoredEvent -> {}
            }
        }

        val idSet = mutableSetOf<String>()
        merge(eventsLoader, additionalEventsLoader ?: emptyFlow())
            .logAndRetryWithDelay(5.seconds) {
                logger.error("Exception caught in CLICS loader. Will restart in 5 seconds.", it)
                preloadFinished = false
            }
            .sortedPrefix()
            .filterNot { it.token in idSet }
            .onEach { processEvent(it) }
            .takeWhile { !it.isFinalEvent }
            .onEach { idSet.add(it.token) }
            .logAndRetryWithDelay(5.seconds) {
                logger.error("Exception caught in CLICS parser. Will restart in 5 seconds.", it)
                preloadFinished = false
            }.collect()
    }

    override fun getFlow() = flow {
        emit(InfoUpdate(model.contestInfo))
        runLoader(
            onRun = { emit(RunUpdate(it)) },
            onContestInfo = { emit(InfoUpdate(it)) },
            onComment = { emit(AnalyticsUpdate(it)) }
        )
        if (model.contestInfo.status != ContestStatus.FINALIZED) {
            logger.info("Events are finished, while contest is not finalized. Enforce finalization.")
            emit(InfoUpdate(model.contestInfo.copy(status = ContestStatus.FINALIZED)))
        }
    }

    companion object {
        val logger = getLogger(ClicsDataSource::class)
        @OptIn(ExperimentalSerializationApi::class)
        private fun getEventFeedLoader(settings: ParsedClicsLoaderSettings, networkSettings: NetworkSettings?) = flow {
            val jsonDecoder = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }

            while (true) {
                emitAll(getLineStreamLoaderFlow(networkSettings, settings.auth, settings.eventFeedUrl)
                    .filter { it.isNotEmpty() }
                    .mapNotNull { data ->
                        try {
                            when (settings.feedVersion) {
                                ClicsSettings.FeedVersion.`2020_03` -> Event.fromV1(jsonDecoder.decodeFromString(data))
                                ClicsSettings.FeedVersion.`2022_07` -> jsonDecoder.decodeFromString<Event>(data)
                            }
                        } catch (e: SerializationException) {
                            logger.error("Failed to deserialize: $data", e)
                            null
                        }
                    })
                if (!isHttpUrl(settings.eventFeedUrl)) { break }
                delay(5.seconds)
                logger.info("Connection ${settings.eventFeedUrl} is closed, retrying")
            }
        }
    }
}
