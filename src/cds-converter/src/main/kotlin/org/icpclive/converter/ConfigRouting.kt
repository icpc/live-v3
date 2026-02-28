package org.icpclive.converter

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.cds.api.AccountInfo
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.util.sendJsonFlow
import java.nio.file.Path
import kotlin.io.path.notExists

class ApiActionException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
class ActionResponse<T>(
    val status: String,
    val response: T?
)

suspend inline fun <reified T> ApplicationCall.adminApiAction(
    block: ApplicationCall.() -> T
) = try {
    val user = principal<AccountInfo>()
    application.log.info("Changing request ${request.path()} is done by ${user?.username}")
    val result = block()
    respondText(contentType = ContentType.Application.Json) {
        Json.encodeToString(
            ActionResponse.serializer(serializer<T>()),
            ActionResponse("ok", result.takeUnless { it is Unit })
        )
    }
} catch (e: ApiActionException) {
    respond(HttpStatusCode.BadRequest, mapOf("status" to "error", "message" to e.message))
}

fun Route.configureConfigFileRouting(
    path: Path,
    emptyResponse: String,
    validate: (String) -> Unit,
    schemaLocation: String?,
    examplesPackage: String?
) {
    get {
        if (path.notExists()) {
            call.respondText(emptyResponse)
        } else {
            call.respondFile(path.toFile())
        }
    }
    post {
        call.adminApiAction {
            val text = call.receiveText()
            validate(text)
            path.toFile().writeText(text)
        }
    }
    get("/schema") {
        if (schemaLocation != null) {
            call.respondRedirect(schemaLocation)
        } else {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    if (examplesPackage != null) {
        staticResources("/examples", examplesPackage)
    } else {
        get("/examples/descriptions.json") {
            call.respondText("{}", ContentType.Application.Json)
        }
    }
}

inline fun <reified T : Any> Route.flowEndpoint(name: String, crossinline dataProvider: suspend (ApplicationCall) -> Flow<T>?) {
    webSocket(name) {
        val flow = dataProvider(call) ?: return@webSocket
        sendJsonFlow(flow)
    }
    get(name) {
        val result = dataProvider(call)?.first() ?: return@get
        call.respond(result)
    }
}
