package org.icpclive.sniper

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.icpclive.util.getLogger

fun Route.setupRouting() {
    post("/move") {
        call.adminApiAction {
            logger.info("hello!!!")
            val request = call.receive<SniperRequest>()
            SniperMover.moveToTeam(request.sniperId, request.teamId)
            Unit
        }
    }

    post("/show_with_settings") {
        call.adminApiAction {
            val request = call.receive<SniperRequest>()
            val locatorSettings = LocatorController.getLocatorWidgetConfig(request.sniperId, setOf(request.teamId))
            println("request: " + "${config.overlayURL}/api/admin/teamLocator/show_with_settings")
            val showRequest = Util.httpClient.post("${config.overlayURL}/api/admin/teamLocator/show_with_settings") {
                setBody(Json.encodeToString(locatorSettings))
                contentType(ContentType.Application.Json)
            }
            val response = showRequest.bodyAsText()
            println("response (${showRequest.status}): $response")
            response
        }
    }

    post("/hide") {
        call.adminApiAction {
            val response = Util.httpClient.post("${config.overlayURL}/api/admin/teamLocator/hide").bodyAsText()
            println(response)
        }
    }

    get("/snipers") {
        val ids = SnipersID(ArrayList())
        for (sniper in Util.snipers) {
            ids.ids.add(sniper.cameraID)
        }
        call.respond(ids)
    }

    get("/teams") {
        call.respondBytes(
            Util.httpClient.get("${config.overlayURL}/api/admin/teamView/teams").readBytes(),
            contentType = ContentType.Application.Json,
        )
    }

}

@Serializable
@Suppress("unused")
class ActionResponse<T>(
    val status: String,
    val response: T?
)

suspend inline fun <reified T> ApplicationCall.adminApiAction(
    block: ApplicationCall.() -> T
) = try {
    val result = block()
    respondText(contentType = ContentType.Application.Json) {
        Json.encodeToString(
            ActionResponse.serializer(serializer<T>()),
            ActionResponse("ok", result.takeUnless { it is Unit })
        )
    }
} catch (e: Exception) {
    logger.info("Failed to run admin call $e")
    println("Failed to run admin call ${e.message}")
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}

val logger = getLogger(ApplicationCall::class)
