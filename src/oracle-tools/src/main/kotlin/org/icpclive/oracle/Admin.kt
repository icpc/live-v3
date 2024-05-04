package org.icpclive.oracle

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
import org.icpclive.cds.util.getLogger

fun Route.setupRouting() {
    post("/move") {
        call.adminApiAction {
            logger.info("hello!!!")
            val request = call.receive<OracleRequest>()
            OracleMover.moveToTeam(request.oracleId, request.teamId)
            Unit
        }
    }

    post("/show_with_settings") {
        call.adminApiAction {
            val request = call.receive<OracleRequest>()
            val locatorSettings = LocatorController.getLocatorWidgetConfig(request.oracleId, setOf(request.teamId))
            println("request: " + "${config.overlayURL}/api/admin/teamLocator/show_with_settings")
            val showRequest = Util.httpClient.post("${config.overlayURL}/api/admin/teamLocator/show_with_settings") {
                headers {
                    append(HttpHeaders.Authorization, BasicAuthKey.key)
                }
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
            val response = Util.httpClient.post("${config.overlayURL}/api/admin/teamLocator/hide") {
                headers {
                    append(HttpHeaders.Authorization, BasicAuthKey.key)
                }
            }.bodyAsText()
            println(response)
        }
    }

    get("/oracles") {
        val ids = OraclesID(ArrayList())
        for (oracle in Util.oracles) {
            ids.ids.add(oracle.cameraID)
        }
        call.respond(ids)
    }

    get("/teams") {
        call.respondBytes(
            Util.httpClient.get("${config.overlayURL}/api/admin/teamView/teams") {
                headers {
                    append(HttpHeaders.Authorization, BasicAuthKey.key)
                }
            }.readBytes(),
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
