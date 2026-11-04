package org.icpclive.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.LocalPathContent
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.contentType
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.loadConfigFile
import org.icpclive.util.sendFlow
import java.nio.file.Path
import kotlin.io.path.notExists

public fun Route.configureConfigFileRouting(
    path: Path,
    emptyResponse: String,
    contentType: ContentType,
    validate: (String) -> Unit,
    schemaLocation: String?,
    examplesPackage: String?
) {
    get {
        if (path.notExists()) {
            call.respondText(emptyResponse, contentType)
        } else {
            call.respond(LocalPathContent(path, contentType = contentType))
        }
    }
    if (contentType == ContentType.Application.Json) {
        get("/json") {
            if (path.notExists()) {
                call.respondText(emptyResponse, contentType)
            } else {
                withContext(Dispatchers.IO) {
                    call.respond(loadConfigFile<JsonElement>(path.toFile()))
                }
            }
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


public fun Route.configureDefaultConfigRouting(
    settingsJonsPath: Path,
    advancedJsonPath: Path,
    visualConfigFile: Path,
    customFieldsCsvPath: Path,
    orgCustomFieldsCsvPath: Path,
    contestInfoFlowProvider: suspend ApplicationCall.() -> Flow<ContestInfo>
) {
    route("/settings") {
        configureConfigFileRouting(
            settingsJonsPath,
            emptyResponse = "{}",
            contentType = ContentType.Application.Json,
            validate = { throw ApiActionException("Settings file can't be modified") },
            schemaLocation = "/schemas/settings.schema.json",
            examplesPackage = null
        )
    }
    route("/advancedJson") {
        configureConfigFileRouting(
            advancedJsonPath,
            emptyResponse = "[]",
            contentType = ContentType.Application.Json,
            validate = {
                try {
                    // check if parsable
                    val _ = TuningRule.listFromString(it)
                } catch (e: SerializationException) {
                    throw ApiActionException("Failed to deserialize advanced.json: ${e.message}", e)
                }
            },
            schemaLocation = "/schemas/advanced.schema.json",
            examplesPackage = "examples.advanced"
        )
    }

    route("/visualConfig") {
        configureConfigFileRouting(
            visualConfigFile,
            emptyResponse = "{}",
            contentType = ContentType.Application.Json,
            validate = { },
            schemaLocation = "/schemas/visual-config.schema.json",
            examplesPackage = "examples.visual"
        )
    }

    route("/customFields") {
        configureConfigFileRouting(
            customFieldsCsvPath,
            emptyResponse = "",
            contentType = ContentType.Text.CSV,
            validate = { },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    route("/orgCustomFields") {
        configureConfigFileRouting(
            orgCustomFieldsCsvPath,
            emptyResponse = "",
            contentType = ContentType.Text.CSV,
            validate = { },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    flowEndpoint("contestInfo", contestInfoFlowProvider)
    webSocket("/backendLog") { sendFlow(AdminDataBus.loggerFlow) }
    webSocket("/adminActions") { sendFlow(AdminDataBus.adminActionsFlow) }
}