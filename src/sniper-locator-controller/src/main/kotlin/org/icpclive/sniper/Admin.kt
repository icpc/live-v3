package org.icpclive.sniper

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

fun Route.setupRouting() {
    post("/move") {
        call.adminApiAction {
            val request = call.receive<SniperRequest>()
            SniperMover.moveToTeam(request.sniperId, request.teamId)
            Unit
        }
    }

    post("/show_with_settings") {
        call.adminApiAction {
            val request = call.receive<SniperRequest>()
            val locatorSettings = LocatorController.getLocatorWidgetConfig(request.sniperId, setOf(request.teamId))
            val response = Util.httpClient.post("${config.overlayURL}/api/admin/locator/show_with_settings") {
                setBody(locatorSettings)
            }.bodyAsText()
            println(response)
        }
    }

    post("/hide") {
        call.adminApiAction {
            val response = Util.httpClient.post("${config.overlayURL}/api/admin/locator/hide").bodyAsText()
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
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}