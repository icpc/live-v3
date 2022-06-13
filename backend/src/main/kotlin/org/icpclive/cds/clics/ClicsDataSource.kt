package org.icpclive.cds.clics

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.clics.api.*
import org.icpclive.config.Config
import org.icpclive.service.EventFeedLoaderService
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import kotlin.time.Duration.Companion.seconds

class ClicsDataSource : ContestDataSource {
    private val properties = Config.loadProperties("events")
    private val central = ClicsApiCentral(properties)
    private val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

    private val model = ClicsModel()
    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }

    fun CoroutineScope.launchLoader() = run {
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
        val rawEventsFlow = MutableSharedFlow<Event>(
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        val contestInfoFlow = MutableStateFlow(model.contestInfo)
        val rawRunsFlow = MutableSharedFlow<RunInfo>(
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        launch(Dispatchers.IO) {
            eventsLoader.run(rawEventsFlow)
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
                            prefix.add(channel.receive())
                        }
                    } catch (e: TimeoutCancellationException) {
                        break
                    }
                }
                val contestEvents = prefix.filterIsInstance<UpdateContestEvent>()
                val runEvents = prefix.filterIsInstance<UpdateRunEvent>()
                val otherEvents = prefix.filter { it !is UpdateContestEvent && it !is UpdateRunEvent }
                contestEvents.sortedBy { priority(it)  }.forEach { emit(it) }
                runEvents.sortedBy { priority(it) }.forEach { emit(it) }
                otherEvents.forEach { emit(it) }
                emit(PreloadFinishedEvent("", Operation.CREATE))
                emitAll(channel)
            }
            var preloadFinished = false
            rawEventsFlow.sortedPrefix().collect {
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
                                    rawRunsFlow.emit(run.toApi())
                                }
                            }
                        }
                        if (preloadFinished) {
                            contestInfoFlow.value = model.contestInfo
                        }
                    }
                    is UpdateRunEvent -> {
                        when (it) {
                            is SubmissionEvent -> model.processSubmission(it.data)
                            is JudgementEvent -> model.processJudgement(it.data)
                            is RunsEvent -> model.processRun(it.data)
                        }.also { run ->
                            if (preloadFinished) {
                                rawRunsFlow.emit(run.toApi())
                            }
                        }
                    }
                    is IgnoredEvent -> {}
                }
            }
        }
        rawRunsFlow to contestInfoFlow
    }

    override suspend fun run() {
        coroutineScope {
            val (rawRunsFlow, contestInfoFlow) = launchLoader()
            launchICPCServices(rawRunsFlow, contestInfoFlow)
        }
    }

    override suspend fun loadOnce() = coroutineScope {
        val (_, contestInfoFlow) = launchLoader()
        val contestInfo = contestInfoFlow.first { it.status == ContestStatus.OVER }
        coroutineContext.cancelChildren()
        val runs = model.submissions.values.map { it.toApi() }
        contestInfo to runs
    }

    companion object {
        val logger = getLogger(ClicsDataSource::class)
    }
}
