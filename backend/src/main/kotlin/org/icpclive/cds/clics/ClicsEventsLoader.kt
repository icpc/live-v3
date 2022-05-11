package org.icpclive.cds.clics

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestStatus
import org.icpclive.api.RunInfo
import org.icpclive.cds.clics.api.*
import org.icpclive.config.Config
import org.icpclive.service.EventFeedLoaderService
import org.icpclive.service.launchEmulation
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat

class ClicsEventsLoader {
    private val properties = Config.loadProperties("events")
    private val central = ClicsApiCentral(properties)
    private val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

    private val model = ClicsModel()
    private val jsonDecoder = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun run() {
        coroutineScope {
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

            val contestInfoFlow = MutableStateFlow(model.contestInfo.toApi())
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )

            launch {
                eventsLoader.run(rawEventsFlow)
            }

            launch {
                rawEventsFlow.collect {
                    when (it) {
                        is UpdateContestEvent -> {
                            when (it) {
                                is ContestEvent -> model.processContest(it.data)
                                is ProblemEvent -> model.processProblem(it.data)
                                is OrganizationEvent -> model.processOrganization(it.data)
                                is TeamEvent -> model.processTeam(it.data)
                                is StateEvent -> model.processState(it.data)
                            }
                            contestInfoFlow.value = model.contestInfo.toApi()
                        }
                        is UpdateRunEvent -> {
                            when (it) {
                                is SubmissionEvent -> model.processSubmission(it.data)
                                is JudgementEvent -> model.processJudgement(it.data)
                            }.also { run -> rawRunsFlow.emit(run.toApi()) }
                        }
                        is IgnoredEvent -> {}
                    }
                }
            }

            if (emulationSpeedProp != null) {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                val contestInfo = contestInfoFlow.first { it.status == ContestStatus.OVER }
                val runs = model.submissions.values.map { it.toApi() }
                launchEmulation(emulationStartTime, emulationSpeed, runs, contestInfo)
            } else {
                launchICPCServices(rawRunsFlow, contestInfoFlow)
            }
        }
    }

    companion object {
        val logger = getLogger(ClicsEventsLoader::class)
    }
}
