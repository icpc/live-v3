package org.icpclive.cds.plugins.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.clics.Url
import org.icpclive.clics.clicsEventsSerializersModule
import org.icpclive.clics.events.*
import org.icpclive.util.getLogger
import org.icpclive.util.logAndRetryWithDelay
import kotlin.time.Duration.Companion.seconds

public enum class FeedVersion {
    `2020_03`,
    `2022_07`,
    `2023_06`
}

@Serializable
public class ClicsFeed(
    @Contextual public val url: UrlOrLocalPath,
    public val contestId: String,
    @Contextual public val login: Credential? = null,
    @Contextual public val password: Credential? = null,
    public val eventFeedName: String = "event-feed",
    public val eventFeedPath: String? = null,
    public val urlPrefixMapping: Map<String, String> = emptyMap(),
    public val feedVersion: FeedVersion = FeedVersion.`2023_06`,
)

@Builder("clics")
public sealed interface ClicsSettings : CDSSettings {
    public val feeds: List<ClicsFeed>
    public val useTeamNames: Boolean
        get() = true

    override fun toDataSource(): ContestDataSource = ClicsDataSource(this)
}

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
    val urlPrefixMapping = settings.urlPrefixMapping
}

internal class ClicsDataSource(val settings: ClicsSettings) : ContestDataSource {
    private val feeds = settings.feeds.map { ParsedClicsLoaderSettings(it) }

    private val model = ClicsModel(
        settings.useTeamNames
    )

    private val Event.isFinalEvent get() = this is StateEvent && data?.endOfUpdates != null

    private suspend fun runLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (AnalyticsCommentaryEvent) -> Unit,
    ) {
        val loaders = feeds.map { getEventFeedLoader(it, settings.network) }

        fun priority(event: UpdateContestEvent) = if (event.isFinalEvent) Int.MAX_VALUE else when (event) {
            is ContestEvent -> 0
            is StateEvent -> 1
            is JudgementTypeEvent -> 2
            is OrganizationEvent -> 3
            is GroupEvent -> 4
            is TeamEvent -> 5
            is ProblemEvent -> 6
            is PreloadFinishedEvent -> throw IllegalStateException()
            is AwardEvent, is LanguageEvent, is AccountEvent -> 8
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
                        is GroupEvent -> model.processGroup(it.id, it.data)
                        is PreloadFinishedEvent -> {
                            preloadFinished = true
                        }

                        is AwardEvent, is LanguageEvent, is AccountEvent -> {}
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
                is ClarificationEvent -> {}
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
                serializersModule = clicsEventsSerializersModule(org.icpclive.clics.FeedVersion.valueOf(settings.feedVersion.name)) {
                    val mapped = settings.urlPrefixMapping.entries.fold(it) { acc, (key, value) ->
                        if (acc.startsWith(key)) {
                            value + acc.substring(key.length)
                        } else {
                            acc
                        }
                    }
                    if (mapped.startsWith("http://") || mapped.startsWith("https://")) {
                        Url(mapped)
                    } else {
                        Url(
                            when (val path = settings.baseUrl.subDir(it)) {
                                is UrlOrLocalPath.Local -> path.value.joinToString("/")
                                is UrlOrLocalPath.Url -> path.value
                            }
                        )
                    }
                }
            }

            while (true) {
                emitAll(getLineStreamLoaderFlow(networkSettings, settings.auth, settings.eventFeedUrl)
                    .filter { it.isNotEmpty() }
                    .mapNotNull { data ->
                        try {
                            jsonDecoder.decodeFromString<Event>(data)
                        } catch (e: SerializationException) {
                            logger.error("Failed to deserialize: $data\n${e.message}\n\n")
                            null
                        }
                    })
                if (settings.eventFeedUrl is UrlOrLocalPath.Local) {
                    break
                }
                delay(5.seconds)
                logger.info("Connection ${settings.eventFeedUrl} is closed, retrying")
            }
        }
    }
}
