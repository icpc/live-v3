package org.icpclive.service

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.common.defaultHttpClient
import org.icpclive.util.getLogger


class ReactionRecordService() {
    private val httpClient = defaultHttpClient(null)

    suspend fun run(flow: Flow<KeyTeam>, info: StateFlow<ContestInfo>) {
        logger.info("ReactionRecordService is started")
        flow.collect {
            val team = info.value.teams.find { t -> t.id == it.teamId }
            println("record for ${team}")
            team ?: return@collect
            record(team.contestSystemId, "webcam")
            record(team.contestSystemId, "desktop")
            delay(500)
        }
    }

    suspend fun record(peerName: String, type: String) {
        try {
            val r = httpClient.request("http://192.168.90.248:8001/record/start") {

                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                val x = "{\"key\": \"\", \"peerName\": \"$peerName\", \"streamType\": \"$type\", \"duration\": 60}"
//            print(x)
//            setBody("{key: \"\", peerName: \"1441\", streamType: \"webcam\", duration: 60}")
                setBody(x)
            }
//        r.content.awaitContent()
            val stringBody: String = r.body()
            println("$r $stringBody")
        } catch (e: Exception) {
            println("failed to run record $peerName $type")
        }
    }

    companion object {
        val logger = getLogger(ReactionRecordService::class)
    }
}


