package org.icpclive.cds.plugins.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.*
import org.icpclive.cds.adapters.autoCreateMissingGroupsAndOrgs
import org.icpclive.cds.api.*
import org.icpclive.cds.ktor.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.logAndRetryWithDelay
import org.icpclive.cds.util.onIdle
import org.icpclive.clics.Url
import org.icpclive.clics.clicsEventsSerializersModule
import org.icpclive.clics.events.*
import org.icpclive.ksp.cds.Builder
import java.net.URI
import kotlin.time.Duration.Companion.seconds

public enum class FeedVersion {
    `2020_03`,
    `2022_07`,
    `2023_06`,
    `2026_01`,
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

    private val Event.isFinalEvent get() = this is StateEvent && data.endOfUpdates != null

    private suspend fun runLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (CommentaryMessage) -> Unit,
    ) {
        var preloadFinished = false
        suspend fun finishPreload() {
            if (preloadFinished) return
            preloadFinished = true
            model.setContestInfoListener(onContestInfo)
            model.setRunInfoListener(onRun)
            model.setCommentaryMessageListener(onComment)
        }

        val idSet = mutableSetOf<EventToken>()
        feeds
            .map { getEventFeedLoader(it, settings.network) }
            .merge()
            .onIdle<Event?>(1.seconds, afterFirst = true) { send(null) }
            .onEach { event ->
                if (event == null) {
                    finishPreload()
                } else if (event.token !in idSet) {
                    model.processEvent(event)
                    event.token?.let { idSet.add(it) }
                }
            }
            .takeWhile { it?.isFinalEvent != true }
            .logAndRetryWithDelay(5.seconds) {
                log.error(it) { "Exception caught in CLICS parser. Will restart in 5 seconds." }
                model.resetListeners()
                preloadFinished = false
            }
            .collect()
        finishPreload()
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
                            when (val path = settings.baseUrl) {
                                is UrlOrLocalPath.Local -> path.subDir(it).value.joinToString("/")
                                is UrlOrLocalPath.Url -> URI(path.value.removeSuffix("/") + "/").resolve(it).toString()
                            }
                        )
                    }
                }
            }

            while (true) {
                emitAll(DataLoader.lineFlow(networkSettings, settings.eventFeedUrl)
                    .logAndRetryWithDelay(5.seconds) {
                        log.error(it) { "There are connection problems with ${settings.eventFeedUrl}. Will retry in 5 seconds." }
                    }
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
