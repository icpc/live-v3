package org.icpclive.cds.yandex

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.yandex.api.ContestDescription
import org.icpclive.cds.yandex.api.Participant
import org.icpclive.cds.yandex.api.Problems
import org.icpclive.config.Config
import org.icpclive.data.DataBus
import org.icpclive.service.RegularLoaderService
import org.icpclive.service.launchEmulation
import org.icpclive.service.launchICPCServices
import org.icpclive.utils.ClientAuth
import org.icpclive.utils.OAuthAuth
import org.icpclive.utils.getLogger
import org.icpclive.utils.guessDatetimeFormat
import java.util.*

class YandexEventLoader  {
    val apiKey: String
    val contestId: Long

    init {
        val props = Config.loadProperties("events")
        apiKey = props.getProperty(TOKEN_PROPERTY_NAME)
        contestId = props.getProperty(CONTEST_ID_PROPERTY_NAME).toLong()
    }

    suspend fun run() {
        val formatter = Json {
            ignoreUnknownKeys  = true
        }

        val contestDescriptionLoader = object : RegularLoaderService<ContestDescription>() {
            override val url = "$API_BASE/contests/$contestId"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                formatter.decodeFromString<ContestDescription>(data)
        }

        val problemLoader = object : RegularLoaderService<Problems>() {
            override val url = "$API_BASE/contests/$contestId/problems"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                formatter.decodeFromString<Problems>(data)
        }

        val participantLoader = object : RegularLoaderService<List<Participant>>() {
            override val url = "$API_BASE/contests/$contestId/participants"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) =
                formatter.decodeFromString<List<Participant>>(data)
        }

        // TODO: update submissions eventually
        val allSubmissionsLoader = object : RegularLoaderService<List<Submission>>() {
            override val url = "$API_BASE/contests/$contestId/submissions?locale=ru&page=1&pageSize=100000"
            override val auth = OAuthAuth(apiKey)
            override fun processLoaded(data: String) = data
        }

        val properties: Properties = Config.loadProperties("events")
        val emulationSpeedProp: String? = properties.getProperty("emulation.speed")

        val contestInfo: ContestInfo = YandexContestInfo(
            contestDescriptionLoader.loadOnce(),
            problemLoader.loadOnce().problems,
            participantLoader.loadOnce()
        ).toApi()
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
        private const val TOKEN_PROPERTY_NAME = "yandex.token"
        private const val CONTEST_ID_PROPERTY_NAME = "yandex.contest_id"
        private const val API_BASE = "https://api.contest.yandex.net/api/public/v2"
    }

}