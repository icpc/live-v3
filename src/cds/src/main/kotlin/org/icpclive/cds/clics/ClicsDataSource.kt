package org.icpclive.cds.clics

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.icpclive.api.AnalyticsCommentaryEvent
import org.icpclive.api.ContestInfo
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.AnalyticsUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.common.ClientAuth
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.common.getLineStreamLoaderFlow
import org.icpclive.cds.settings.ClicsFeed
import org.icpclive.cds.settings.ClicsSettings
import org.icpclive.cds.settings.NetworkSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.clics.clicsEventsSerializersModule
import org.icpclive.clics.v202003.upgrade
import org.icpclive.clics.v202207.Event
import org.icpclive.clics.v202207.Event.*
import org.icpclive.util.getLogger
import org.icpclive.util.logAndRetryWithDelay
import kotlin.time.Duration.Companion.seconds

private class ParsedClicsLoaderSettings(settings: ClicsFeed) {
    val auth = ClientAuth.BasicOrNull(
        settings.login?.value,
        settings.password?.value
    )
    val baseUrl = settings.url
    val eventFeedUrl = buildList {
        if (settings.eventFeedPath != null) {
            if (settings.eventFeedPath.isNotEmpty()) {
                add(settings.eventFeedPath)
            }
        } else {
            add("contests")
            add(settings.contestId)
        }
        add(settings.eventFeedName)
    }.fold(baseUrl, UrlOrLocalPath::subDir)
    val feedVersion = settings.feedVersion
}

internal class ClicsDataSource(val settings: ClicsSettings) : ContestDataSource {
    private val feeds = settings.feeds.map { ParsedClicsLoaderSettings(it) }

    private val model = ClicsModel(
        settings.useTeamNames
    )

    val Event.isFinalEvent get() = this is StateEvent && data?.end_of_updates != null

    suspend fun runLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (AnalyticsCommentaryEvent) -> Unit
    ) {
        val loaders = feeds.map { getEventFeedLoader(it, settings.network) }

        fun priority(event: UpdateContestEvent) = if (event.isFinalEvent) Int.MAX_VALUE else when (event) {
            is ContestEvent -> 0
            is StateEvent -> 1
            is JudgementTypeEvent -> 2
            is OrganizationEvent -> 3
            is GroupsEvent -> 4
            is TeamEvent -> 5
            is ProblemEvent -> 6
            is PreloadFinishedEvent -> throw IllegalStateException()
            is AccountEvent -> 7
            is AwardsEvent -> 8
            is ClarificationEvent -> 9
            is LanguageEvent -> 10
            is MapEvent -> 11
            is PersonEvent -> 12
            is StartStatusEvent -> 13
        }

        fun priority(event: UpdateRunEvent) = when (event) {
            is SubmissionEvent -> 0
            is JudgementEvent -> 1
            is RunEvent -> 2
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
                    when (it) {
                        is ContestEvent -> model.processContest(it.data!!)
                        is ProblemEvent -> model.processProblem(it.id, it.data)
                        is OrganizationEvent -> model.processOrganization(it.id, it.data)
                        is TeamEvent -> model.processTeam(it.id, it.data)
                        is StateEvent -> model.processState(it.data!!)
                        is JudgementTypeEvent -> model.processJudgementType(it.id, it.data)
                        is GroupsEvent -> model.processGroup(it.id, it.data)
                        is PreloadFinishedEvent -> { preloadFinished = true }
                        is AccountEvent, is AwardsEvent, is ClarificationEvent, is LanguageEvent,
                        is MapEvent, is PersonEvent, is StartStatusEvent -> {}
                    }
                    if (preloadFinished) {
                        onContestInfo(model.contestInfo)
                        if (it is PreloadFinishedEvent) {
                            for (run in model.getAllRuns()) {
                                onRun(run)
                            }
                        }
                    }
                }

                is UpdateRunEvent -> {
                    when (it) {
                        is SubmissionEvent -> model.processSubmission(it.data!!)
                        is JudgementEvent -> model.processJudgement(it.id, it.data)
                        is RunEvent -> model.processRun(it.id, it.data)
                    }.also { run ->
                        if (preloadFinished && run != null) {
                            onRun(run)
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
            }
        }

        val idSet = mutableSetOf<String>()
        loaders
            .merge()
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
                serializersModule = SerializersModule {
                    include(clicsEventsSerializersModule { "${settings.baseUrl}/${it}" })
                }
            }

            while (true) {
                emitAll(getLineStreamLoaderFlow(networkSettings, settings.auth, settings.eventFeedUrl)
                    .filter { it.isNotEmpty() }
                    .mapNotNull { data ->
                        try {
                            when (settings.feedVersion) {
                                ClicsSettings.FeedVersion.`2020_03` -> jsonDecoder.decodeFromString<org.icpclive.clics.v202003.Event>(data).upgrade()
                                ClicsSettings.FeedVersion.`2022_07` -> jsonDecoder.decodeFromString<Event>(data)
                            }
                        } catch (e: SerializationException) {
                            logger.error("Failed to deserialize: $data", e)
                            null
                        }
                    })
                if (settings.eventFeedUrl is UrlOrLocalPath.Local) { break }
                delay(5.seconds)
                logger.info("Connection ${settings.eventFeedUrl} is closed, retrying")
            }
        }
    }
}
