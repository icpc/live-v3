package org.icpclive.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.serialization.SerializationException
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.util.sendFlow
import java.nio.file.Path
import kotlin.io.path.notExists

public fun Route.configureConfigFileRouting(
    path: Path,
    emptyResponse: String,
    validate: (String) -> Unit,
    schemaLocation: String?,
    examplesPackage: String?,
    onSave: ((Job) -> Unit)? = null,
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
            try {
                validate(text)
            } catch (e: SerializationException) {
                throw ApiActionException("Failed to parse new text: ${e.message}", e)
            }
            path.toFile().writeText(text)
            onSave?.invoke(call.coroutineContext.job)
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
    contestInfoFlowProvider: suspend ApplicationCall.() -> Flow<ContestInfo>,
    validateSettingsReload: (String) -> Unit,
    onSettingsReload: (Job) -> Unit,
) {
    route("/settings") {
        configureConfigFileRouting(
            settingsJonsPath,
            emptyResponse = "{}",
            validate = validateSettingsReload,
            schemaLocation = "/schemas/settings.schema.json",
            examplesPackage = "examples.settings",
            onSave = onSettingsReload,
        )
    }
    route("/advancedJson") {
        configureConfigFileRouting(
            advancedJsonPath,
            emptyResponse = "[]",
            validate = {
                val _ = TuningRule.listFromString(it)
            },
            schemaLocation = "/schemas/advanced.schema.json",
            examplesPackage = "examples.advanced"
        )
    }

    route("/visualConfig") {
        configureConfigFileRouting(
            visualConfigFile,
            emptyResponse = "{}",
            validate = { },
            schemaLocation = "/schemas/visual-config.schema.json",
            examplesPackage = "examples.visual"
        )
    }

    route("/customFields") {
        configureConfigFileRouting(
            customFieldsCsvPath,
            emptyResponse = "",
            validate = { },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    route("/orgCustomFields") {
        configureConfigFileRouting(
            orgCustomFieldsCsvPath,
            emptyResponse = "",
            validate = { },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    flowEndpoint("contestInfo", contestInfoFlowProvider)
    webSocket("/backendLog") { sendFlow(AdminDataBus.loggerFlow) }
    webSocket("/adminActions") { sendFlow(AdminDataBus.adminActionsFlow) }
}