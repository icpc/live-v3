package org.icpclive.cds.wf2

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.config.Config
import org.icpclive.service.EventFeedLoaderService
import org.icpclive.service.launchICPCServices

class WF2EventsLoader {
    private val properties = Config.loadProperties("events")
    private val central = WF2ApiCentral(properties)

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

            val contestInfoFlow = MutableStateFlow<ContestInfo>(model.contestInfo.toApi())
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
                    when (type) {
                        "contests" -> model::processContest
                        "problems" -> model::processProblem
                        "organizations" -> model::processOrganisation
                        "teams" -> model::processTeam
                        else -> null
                    }?.invoke(data)
                    when (type) {
                        "problems", "organizations", "teams" -> {
                            contestInfoFlow.value = model.contestInfo.toApi()
                        }
                    }
                }
            }

            launchICPCServices(rawRunsFlow, contestInfoFlow)

//            delay(1000)
//            model.problems.forEach { (id, pr) -> println(pr) }
        }
    }
}
