package org.icpclive.cds.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.AnalyticsCommentaryEvent
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.ContestParseResult
import org.icpclive.cds.clics.api.*
import org.icpclive.service.EventFeedLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import org.icpclive.utils.reliableSharedFlow
import java.util.*
import kotlin.time.Duration.Companion.seconds

class ClicsDataSource(properties: Properties) : ContestDataSource {
    private val central = ClicsApiCentral(properties)

    private val model = ClicsModel()
    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val Event.isFinalEvent get() = this is StateEvent && data.end_of_updates != null

    fun CoroutineScope.launchLoader(
        onRun: suspend (RunInfo) -> Unit,
        onContestInfo: suspend (ContestInfo) -> Unit,
        onComment: suspend (AnalyticsCommentaryEvent) -> Unit
    ) {
        val eventsLoader = object : EventFeedLoaderService<Event>(central.auth) {
            val idsSet = mutableSetOf<String>()
            override val url = central.eventFeedUrl
            override fun processEvent(data: String) = try {
                jsonDecoder.decodeFromString<Event>(data).takeIf { idsSet.add(it.id) }
            } catch (e: SerializationException) {
                logger.error("Failed to deserialize: $data")
                null
            }
        }

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
                val channel = produceIn(this@launch)
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
                emit(PreloadFinishedEvent("", Operation.CREATE))
                if (contestEvents.none { it.isFinalEvent }) {
                    for (event in channel) {
                        emit(event)
                        if (event.isFinalEvent) break
                    }
                }
                channel.cancel()
            }

            var preloadFinished = false
            eventsLoader.run().sortedPrefix().collect {
                when (it) {
                    is UpdateContestEvent -> {
                        when (it) {
                            is ContestEvent -> model.processContest(it.data)
                            is ProblemEvent -> model.processProblem(it.op, it.data)
                            is OrganizationEvent -> model.processOrganization(it.op, it.data)
                            is TeamEvent -> model.processTeam(it.op, it.data)
                            is StateEvent -> model.processState(it.data)
                            is JudgementTypeEvent -> model.processJudgementType(it.op, it.data)
                            is GroupsEvent -> model.processGroup(it.op, it.data)
                            is PreloadFinishedEvent -> {
                                preloadFinished = true
                                for (run in model.submissions.values.sortedBy { it.id }) {
                                    onRun(run.toApi())
                                }
                            }
                        }
                        if (preloadFinished) {
                            onContestInfo(model.contestInfo)
                        }
                    }
                    is UpdateRunEvent -> {
                        when (it) {
                            is SubmissionEvent -> model.processSubmission(it.data)
                            is JudgementEvent -> model.processJudgement(it.data)
                            is RunsEvent -> model.processRun(it.data)
                        }.also { run ->
                            if (preloadFinished) {
                                onRun(run.toApi())
                            }
                        }
                    }
                    is CommentaryEvent -> {
                        onComment(
                            AnalyticsCommentaryEvent(
                                it.data.id,
                                it.data.message,
                                it.data.time,
                                it.data.contest_time,
                                it.data.team_ids?.map { model.externalTeamId(it) } ?: emptyList(),
                                it.data.submission_ids?.map { model.externalSubmissionId(it) } ?: emptyList(),
                            )
                        )
                    }
                    is IgnoredEvent -> {}
                }
            }
        }
    }

    override suspend fun run() {
        coroutineScope {
            val contestInfoFlow = MutableStateFlow(model.contestInfo)
            val rawRunsFlow = reliableSharedFlow<RunInfo>()
            val analyticsEventsFlow = reliableSharedFlow<AnalyticsMessage>()
            launchLoader(
                onRun = { rawRunsFlow.emit(it) },
                onContestInfo = { contestInfoFlow.value = it },
                onComment = { analyticsEventsFlow.emit(it) }
            )
            launchICPCServices(rawRunsFlow, contestInfoFlow, analyticsEventsFlow)
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
        val runs = model.submissions.values.map { it.toApi() }
        return ContestParseResult(model.contestInfo, runs, analyticsMessages)
    }

    companion object {
        val logger = getLogger(ClicsDataSource::class)
    }
}
