package org.icpclive.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.data.DataBus

class ApiActionException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
@Suppress("unused")
class ActionResponse<T>(
    val status: String,
    val response: T?
)

suspend inline fun <T> ApplicationCall.adminApiAction(
    responseSerializer: KSerializer<T>,
    block: ApplicationCall.() -> T
) = try {
    val user = principal<User>()
    if (user != null && !user.confirmed) throw ApiActionException("Your account is not confirmed yet")
    application.log.info("Changing request ${request.path()} is done by ${user?.name}")
    val result = block()
    respondText(contentType = ContentType.Application.Json) {
        Json.encodeToString(
            ActionResponse.serializer(responseSerializer),
            ActionResponse("ok", result.takeUnless { it is Unit })
        )
    }
    DataBus.adminActionsFlow.emit(request.uri)
} catch (e: ApiActionException) {
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}

suspend inline fun <reified T> ApplicationCall.adminApiAction(block: ApplicationCall.() -> T) =
    adminApiAction(serializer(), block)


suspend inline fun <reified T> ApplicationCall.safeReceive(): T = try {
    receive()
} catch (e: SerializationException) {
    throw ApiActionException("Failed to deserialize data: ${e.message}", e)
}
