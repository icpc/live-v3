package org.icpclive.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.staticResources
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.flow.Flow
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
    /** Runs after the file was written. When set, a `POST reload` endpoint is exposed too. */
    afterSave: (suspend () -> Unit)? = null,
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
            afterSave?.invoke()
        }
    }
    if (afterSave != null) {
        post("/reload") {
            call.adminApiAction { afterSave() }
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
    /**
     * Applies a changed settings file. Settings decide which CDS is used, so there is no way to
     * pick them up other than rebuilding everything. When not given, settings.json stays read-only.
     */
    onSettingsReload: (suspend () -> Unit)? = null,
) {
    route("/settings") {
        configureConfigFileRouting(
            settingsJonsPath,
            emptyResponse = "{}",
            validate = if (onSettingsReload == null) {
                { throw ApiActionException("Settings file can't be modified") }
            } else {
                { }
            },
            schemaLocation = "/schemas/settings.schema.json",
            examplesPackage = null,
            afterSave = onSettingsReload,
        )
    }
    route("/advancedJson") {
        configureConfigFileRouting(
            advancedJsonPath,
            emptyResponse = "[]",
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