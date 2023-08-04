package org.icpclive

import ClicsExporter
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.tunning.AdvancedProperties
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.cds.settings.parseFileToCdsSettings
import org.icpclive.util.*
import org.icpclive.org.icpclive.export.pcms.PCMSExporter
import org.slf4j.event.Level
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.exists
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

private fun Application.setupKtorPlugins() {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(StatusPages) {
        exception<Throwable> { call, ex ->
            call.application.environment.log.error("Query ${call.url()} failed with exception", ex)
            throw ex
        }
    }
    install(AutoHeadResponse)
    install(IgnoreTrailingSlash)
    install(ContentNegotiation) { json(defaultJsonSettings()) }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CORS) {
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("*")
        allowMethod(HttpMethod.Delete)
        allowSameOrigin = true
        anyHost()
    }
}

@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupKtorPlugins()

    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        exitProcess(1)
    }
    val configDirectory = environment.config.property("live.configDirectory").getString().let {
        Paths.get(it).toAbsolutePath()
    }
    environment.log.info("Using config directory $configDirectory")
    environment.log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")

    val advancedProperties = fileJsonContentFlow<AdvancedProperties>(configDirectory.resolve("advanced.json"), environment.log)
        .stateIn(this + handler, SharingStarted.Eagerly, AdvancedProperties())

    val creds: Map<String, String> = environment.config.propertyOrNull("live.credsFile")?.getString()?.let {
        Json.decodeFromStream(File(it).inputStream())
    } ?: emptyMap()

    val path = configDirectory.resolve("events.properties")
            .takeIf { it.exists() }
            ?.also { environment.log.warn("Using events.properties is deprecated, use settings.json instead.") }
            ?: configDirectory.resolve("settings.json")


    val loaded = parseFileToCdsSettings(path)
        .toFlow(creds)
        .applyAdvancedProperties(advancedProperties)
        .filterUseless()
        .processHiddenTeamsAndGroups()
        .shareIn(this + handler, SharingStarted.Eagerly, Int.MAX_VALUE)

    val runs = loaded.filterIsInstance<RunUpdate>().map { it.newInfo }
    val contestState = loaded
        .stateWithGroupedRuns { it.teamId }
        .stateIn(this + handler, SharingStarted.Eagerly, ContestStateWithGroupedRuns(null, persistentMapOf()))
    val contestInfo = loaded
        .filterIsInstance<InfoUpdate>()
        .map { it.newInfo }
        .distinctUntilChanged()
        .shareIn(this + handler, SharingStarted.Eagerly, 1)

    routing {
        with (ClicsExporter) {
            route("/clics") {
                setUp(application + handler, contestInfo, runs)
            }
        }
        with (PCMSExporter) {
            route("/pcms") {
                setUp(contestState)
            }
        }
    }

    log.info("Configuration is done")
}
