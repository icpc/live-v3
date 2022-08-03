package org.icpclive.admin

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.icpclive.data.DataBus

class ApiActionException(message: String) : Exception(message)

@Serializable
@Suppress("unused")
class ActionResponse<T>(
    val status: String,
    val response: T? = null
)

suspend inline fun <T> ApplicationCall.adminApiAction(
    responseSerializer: KSerializer<T>,
    block: ApplicationCall.() -> T
) = try {
    val user = principal<User>()!!
    if (!user.confirmed) throw ApiActionException("Your account is not confirmed yet")
    application.log.info("Changing request ${request.path()} is done by ${user.name}")
    val result = block()
    respondText(contentType = ContentType.Application.Json) {
        Json.encodeToString(
            ActionResponse.serializer(responseSerializer), when (result) {
                is Unit -> ActionResponse("ok")
                else -> ActionResponse("ok", result)
            }
        )
    }
    DataBus.adminActionsFlow.emit(request.uri)
} catch (e: ApiActionException) {
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}

suspend inline fun <reified T> ApplicationCall.adminApiAction(block: ApplicationCall.() -> T) =
    adminApiAction(serializer(), block)


suspend inline fun <T> ApplicationCall.safeReceive(serializer: KSerializer<T>): T = try {
    Json.decodeFromString(serializer, receiveText())
} catch (e: SerializationException) {
    throw ApiActionException("Failed to deserialize data")
}

suspend inline fun <reified T> ApplicationCall.safeReceive(): T = safeReceive(serializer())
