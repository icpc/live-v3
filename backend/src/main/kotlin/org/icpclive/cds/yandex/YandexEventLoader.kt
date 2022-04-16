package org.icpclive.cds.yandex

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.icpclive.cds.yandex.api.Participant
import org.icpclive.config.Config
import org.icpclive.service.RegularLoaderService
import org.icpclive.utils.OAuthAuth
import org.icpclive.utils.getLogger

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
    }

    companion object {
        private val log = getLogger(YandexEventLoader::class)
        private const val YANDEX_API_KEY_PROPERTY_NAME = "yandex.api.key"
    }

}