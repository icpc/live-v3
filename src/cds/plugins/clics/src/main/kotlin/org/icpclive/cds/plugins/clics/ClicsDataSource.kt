package org.icpclive.cds.plugins.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.adapters.autoCreateMissingGroupsAndOrgs
import org.icpclive.cds.api.CommentaryMessage
import org.icpclive.ksp.cds.Builder
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.util.*
import org.icpclive.clics.Url
import org.icpclive.clics.clicsEventsSerializersModule
import org.icpclive.clics.events.*
import kotlin.time.Duration.Companion.seconds

public enum class FeedVersion {
    `2020_03`,
    `2022_07`,
    `2023_06`,
    DRAFT
}

@Serializable
public class ClicsFeed(
    @Contextual public val source: UrlOrLocalPath,
    public val contestId: String,
    public val eventFeedName: String = "event-feed",
    public val eventFeedPath: String? = null,
    public val urlPrefixMapping: Map<String, String> = emptyMap(),
    public val feedVersion: FeedVersion = FeedVersion.`2023_06`,
)

@Builder("clics")
public sealed interface ClicsSettings : CDSSettings, KtorNetworkSettingsProvider {
    public val feeds: List<ClicsFeed>
    override fun toDataSource(): ContestDataSource = ClicsDataSource(this)
}

private class ParsedClicsLoaderSettings(settings: ClicsFeed, val tokenPrefix: String) {
    val baseUrl = settings.source
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
    private val feeds = settings.feeds.mapIndexed { index, it -> ParsedClicsLoaderSettings(it, "feed${index}$") }

    private val model = ClicsModel()

    private val Event.isFinalEvent get() = this is StateEvent && data?.endOfUpdates != null

    private suspend fun runLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (CommentaryMessage) -> Unit,
    ) {
        val loaders = feeds.map { getEventFeedLoader(it, settings.network) }

        fun priority(event: Event) = if (event.isFinalEvent) Int.MAX_VALUE else when (event) {
            // events about contest info
            is ContestEvent -> 0
            is StateEvent -> 1
            is JudgementTypeEvent, is BatchJudgementTypeEvent -> 2
            is LanguageEvent, is BatchLanguageEvent -> 3
            is OrganizationEvent, is BatchOrganizationEvent -> 4
            is GroupEvent, is BatchGroupEvent -> 5
            is TeamEvent, is BatchTeamEvent -> 6
            is ProblemEvent, is BatchProblemEvent -> 7
            is PreloadFinishedEvent -> throw IllegalStateException()
            is AwardEvent, is BatchAwardEvent -> 8
            is AccountEvent, is BatchAccountEvent -> 8
            is PersonEvent, is BatchPersonEvent -> 8
            is BatchClarificationEvent, is ClarificationEvent -> 8
            // events about runs
            is SubmissionEvent, is BatchSubmissionEvent -> 100
            is JudgementEvent, is BatchJudgementEvent -> 101
            is RunEvent, is BatchRunEvent -> 102
            // events about comments
            is CommentaryEvent, is BatchCommentaryEvent -> 200
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
                val events = prefix.sortedBy { priority(it) }
                events.filter { !it.isFinalEvent }.forEach { emit(it) }
                emit(PreloadFinishedEvent(EventToken("")))
                events.filter { it.isFinalEvent }.forEach { emit(it) }
                for (event in channel) {
                    emit(event)
                }
            }
        }

        var preloadFinished = false

        suspend fun processEvent(it: Event) {
            if (it is PreloadFinishedEvent) {
                if (!preloadFinished) {
                    preloadFinished = true
                    model.addContestInfoListener(onContestInfo)
                    model.addRunInfoListener(onRun)
                    model.addCommentaryMessageListener(onComment)
                }
            } else {
                model.processEvent(it)
            }
        }

        val idSet = mutableSetOf<EventToken>()
        loaders
            .merge()
            .logAndRetryWithDelay(5.seconds) {
                log.error(it) { "Exception caught in CLICS loader. Will restart in 5 seconds." }
                preloadFinished = false
            }
            .sortedPrefix()
            .filterNot { it.token in idSet }
            .onEach { processEvent(it) }
            .takeWhile { !it.isFinalEvent }
            .onEach { it.token?.let(idSet::add) }
            .logAndRetryWithDelay(5.seconds) {
                log.error(it) { "Exception caught in CLICS parser. Will restart in 5 seconds." }
                preloadFinished = false
            }.collect()
    }

    override fun getFlow() = flow {
        emit(InfoUpdate(model.contestInfo))
        runLoader(
            onRun = { emit(RunUpdate(it)) },
            onContestInfo = { emit(InfoUpdate(it)) },
            onComment = { emit(CommentaryMessagesUpdate(it)) }
        )
        if (model.contestInfo.status !is ContestStatus.FINALIZED) {
            log.info { "Events are finished, while contest is not finalized. Enforce finalization." }
            emit(InfoUpdate(model.contestInfo.copy(status = ContestStatus.FINALIZED(
                startedAt = model.contestInfo.startTimeOrZero,
                finishedAt = model.contestInfo.startTimeOrZero + model.contestInfo.contestLength,
                frozenAt = model.contestInfo.freezeTime?.let {  model.contestInfo.startTimeOrZero + it },
                finalizedAt = model.contestInfo.startTimeOrZero + model.contestInfo.contestLength,
            ))))
        }
    }.autoCreateMissingGroupsAndOrgs() // for countries

    companion object {
        val log by getLogger()

        private fun getEventFeedLoader(settings: ParsedClicsLoaderSettings, networkSettings: NetworkSettings) = flow {
            val jsonDecoder = Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                serializersModule = clicsEventsSerializersModule(
                    feedVersion = org.icpclive.clics.FeedVersion.valueOf(settings.feedVersion.name),
                    tokenPrefix = settings.tokenPrefix,
                ) {
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
                emitAll(DataLoader.lineFlow(networkSettings, settings.eventFeedUrl)
                    .filter { it.isNotEmpty() }
                    .mapNotNull { data ->
                        try {
                            jsonDecoder.decodeFromString<Event>(data)
                        } catch (e: SerializationException) {
                            log.error { "Failed to deserialize: $data\n${e.message}\n\n" }
                            null
                        }
                    })
                if (settings.eventFeedUrl is UrlOrLocalPath.Local) {
                    break
                }
                delay(5.seconds)
                log.info { "Connection ${settings.eventFeedUrl} is closed, retrying" }
            }
        }
    }
}
