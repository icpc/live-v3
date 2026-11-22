package org.icpclive.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.loadConfigFile
import org.icpclive.cds.util.writeContentAtomically
import org.icpclive.util.sendFlow
import java.nio.file.Path
import kotlin.io.path.notExists

public fun Route.configureConfigFileRouting(
    path: Path,
    emptyResponse: String,
    contentType: ContentType,
    sanitize: (String) -> String,
    schemaLocation: String?,
    examplesPackage: String?,
    onSave: ((Job) -> Unit)? = null,
) {
    get {
        if (path.notExists()) {
            call.respondText(emptyResponse, contentType)
        } else {
            call.respond(LocalPathContent(path, contentType = contentType))
        }
    }
    post {
        call.adminApiAction {
            val text = call.receiveText()
            val sanitizedText = try {
                sanitize(text)
            } catch (e: SerializationException) {
                throw ApiActionException("New text validation failed: ${e.message}", e)
            }
            path.writeContentAtomically(sanitizedText)
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
    sanitizeSettings: (String) -> String,
    onSettingsReload: (Job) -> Unit,
) {
    route("/settings") {
        // settings.json holds credentials, and advanced.json can rewrite the whole contest,
        // so both are admin-only to read as well as to write.
        requireConfirmedAdmin()
        configureConfigFileRouting(
            settingsJonsPath,
            emptyResponse = "{}",
            contentType = ContentType.Application.Json,
            sanitize = sanitizeSettings,
            schemaLocation = "/schemas/settings.schema.json",
            examplesPackage = "examples.settings",
            onSave = onSettingsReload,
        )
    }
    route("/advancedJson") {
        requireConfirmedAdmin()
        configureConfigFileRouting(
            advancedJsonPath,
            emptyResponse = "[]",
            contentType = ContentType.Application.Json,
            sanitize = {
                val _ = TuningRule.listFromString(it)
                it
            },
            schemaLocation = "/schemas/advanced.schema.json",
            examplesPackage = "examples.advanced"
        )
    }

    route("/visualConfig") {
        requireConfirmedAdmin()
        configureConfigFileRouting(
            visualConfigFile,
            emptyResponse = "{}",
            contentType = ContentType.Application.Json,
            sanitize = { it },
            schemaLocation = "/schemas/visual-config.schema.json",
            examplesPackage = "examples.visual"
        )
    }

    route("/customFields") {
        requireConfirmedAdmin()
        configureConfigFileRouting(
            customFieldsCsvPath,
            emptyResponse = "",
            contentType = ContentType.Text.CSV,
            sanitize = { it },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    route("/orgCustomFields") {
        requireConfirmedAdmin()
        configureConfigFileRouting(
            orgCustomFieldsCsvPath,
            emptyResponse = "",
            contentType = ContentType.Text.CSV,
            sanitize = { it },
            schemaLocation = null,
            examplesPackage = null
        )
    }
    // The admin ui itself needs parts of the visual config for every authorized user, not just
    // admins: the branding, and which menu entries and controls to hide. Those hide lists must
    // not be admin-only - failing to read them shows more ui rather than less - so this read is
    // separate from the admin-only /visualConfig editor route.
    get("/uiConfig") {
        if (visualConfigFile.notExists()) {
            call.respondText("{}", ContentType.Application.Json)
        } else {
            withContext(Dispatchers.IO) {
                call.respond(loadConfigFile<JsonElement>(visualConfigFile))
            }
        }
    }
    configureWhoAmIRouting()
    flowEndpoint("contestInfo", contestInfoFlowProvider)
    webSocket("/backendLog") { sendFlow(AdminDataBus.loggerFlow) }
    webSocket("/adminActions") { sendFlow(AdminDataBus.adminActionsFlow) }
}