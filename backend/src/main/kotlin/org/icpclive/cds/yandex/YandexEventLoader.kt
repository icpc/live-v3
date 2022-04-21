package org.icpclive.cds.yandex

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.yandex.api.Participant
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.service.EmulationService
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchEmulation
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.OAuthAuth
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import org.icpclive.utils.humanReadable
import java.util.*

class YandexEventLoader  {
    val apiKey: String
    val contestId: Int

    init {
        val props = Config.loadProperties("events")
        apiKey = props.getProperty(YANDEX_API_KEY_PROPERTY_NAME)
        contestId = props.getProperty("contest_id").toInt()
    }

    suspend fun run() {
        val formatter = Json {
            ignoreUnknownKeys  = true
        }
        val participantLoader = object : RegularLoaderService<List<Participant>>() {
            override val url = "https://api.contest.yandex.net/api/public/v2/contests/$contestId/participants"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) = formatter.decodeFromString<List<Participant>>(data)
        }
        val submissionsLoader = object : RegularLoaderService<String>() {
            override val url = "https://api.contest.yandex.net/api/public/v2/contests/$contestId/submissions?locale=ru&page=1&pageSize=100000"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) = data
        }
        val properties: Properties = Config.loadProperties("events")
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")
        val contestInfo : ContestInfo = TODO()
        val contestInfoFlow = MutableStateFlow(contestInfo).also { DataBus.contestInfoUpdates.complete(it) }

        val rawRunsFlow = MutableSharedFlow<RunInfo>(
            extraBufferCapacity = Int.MAX_VALUE,
            onBufferOverflow = BufferOverflow.SUSPEND
        )

        if (emulationSpeedProp != null) {
            coroutineScope {
                val emulationSpeed = emulationSpeedProp.toDouble()
                val emulationStartTime = guessDatetimeFormat(properties.getProperty("emulation.startTime"))
                launchEmulation(emulationStartTime, emulationSpeed, TODO(), contestInfo)
            }
        } else {
            coroutineScope {
                launchICPCServices(rawRunsFlow, contestInfoFlow)
            }
        }

    }

    companion object {
        private val log = getLogger(YandexEventLoader::class)
        private const val YANDEX_API_KEY_PROPERTY_NAME = "yandex.api.key"
    }

}