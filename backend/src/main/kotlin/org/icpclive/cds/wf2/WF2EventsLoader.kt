package org.icpclive.cds.wf2

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.icpclive.api.RunInfo
import org.icpclive.config.Config
import org.icpclive.service.EventFeedLoaderService
import org.icpclive.service.launchEmulation
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.guessDatetimeFormat

class WF2EventsLoader {
    private val properties = Config.loadProperties("events")
    private val central = WF2ApiCentral(properties)
    private val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

    private val model = WF2Model()

    suspend fun run() {
        coroutineScope {
            val eventsLoader = object : EventFeedLoaderService<JsonObject>(central.auth) {
                override val url = central.eventFeedUrl
                override fun processEvent(data: String) = try {
                    Json.parseToJsonElement(data).jsonObject
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            val rawEventsFlow = MutableSharedFlow<JsonObject>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )

            val contestInfoFlow = MutableStateFlow(model.contestInfo.toApi())
            val rawRunsFlow = MutableSharedFlow<RunInfo>(
                extraBufferCapacity = Int.MAX_VALUE,
                onBufferOverflow = BufferOverflow.SUSPEND
            )

            launch {
                eventsLoader.loadOnce(rawEventsFlow)
            }

            launch {
                rawEventsFlow.collect {
                    val type = it["type"]?.jsonPrimitive?.contentOrNull
                    val data = it["data"]?.jsonObject ?: throw IllegalArgumentException("Event has no data field")
                    if (type in setOf("contests", "problems", "organizations", "teams")) {
                        when (type) {
                            "contests" -> model::processContest
                            "problems" -> model::processProblem
                            "organizations" -> model::processOrganisation
                            "teams" -> model::processTeam
                            else -> null
                        }?.invoke(data)
                        contestInfoFlow.value = model.contestInfo.toApi()
                        return@collect
                    }
                    when (type) {
                        "submissions" -> model::processSubmission
                        "judgements" -> model::processJudgement
                        else -> null
                    }?.invoke(data)?.let { run -> rawRunsFlow.emit(run.toApi()) }
                }
            }

            if (emulationSpeedProp != null) {
                println("loading emulation...")
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                delay(2000)
                println("emulation loaded")
                val runs = model.submissions.values.map { it.toApi() }
                launchEmulation(emulationStartTime, emulationSpeed, runs, model.contestInfo.toApi())
            } else {
                launchICPCServices(rawRunsFlow, contestInfoFlow)
            }
//            delay(1000)
//            model.problems.forEach { (id, pr) -> println(pr) }
        }
    }
}
