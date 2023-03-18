package org.icpclive

import ClicsExporter
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.AdvancedProperties
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.util.*
import org.icpclive.cds.getContestDataSource
import org.icpclive.org.icpclive.export.pcms.PCMSExporter
import org.slf4j.event.Level
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*
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
    val path = configDirectory.resolve("events.properties")
    if (!Files.exists(path)) throw FileNotFoundException("events.properties not found in $configDirectory")
    val properties = Properties()
    FileInputStream(path.toString()).use { properties.load(it) }


    val advancedPropertiesDeferred = CompletableDeferred<Flow<AdvancedProperties>>()
    val contestInfoDeferred = CompletableDeferred<StateFlow<ContestInfo>>()
    val runsDeferred = CompletableDeferred<Flow<RunInfo>>()
    val runsCollectedDeferred = CompletableDeferred<StateFlow<PersistentMap<Int, RunInfo>>>()

    launch(handler) {
        launch {
            advancedPropertiesDeferred.complete(
                fileJsonContentFlow<AdvancedProperties>(configDirectory.resolve("advanced.json"), environment.log)
                    .stateIn(this, SharingStarted.Eagerly, AdvancedProperties())
            )
        }
        launch {
            getContestDataSource(
                properties,
                environment.config.propertyOrNull("live.credsFile")?.getString()?.let {
                    Json.decodeFromStream(File(it).inputStream())
                } ?: emptyMap(),
                calculateFTS = false,
                calculateDifference = false,
                removeFrozenResults = false,
                advancedPropertiesDeferred = advancedPropertiesDeferred
            ).run(contestInfoDeferred, runsDeferred, CompletableDeferred())
        }
        launch {
            runsCollectedDeferred.complete(
                runsDeferred.await().runningFold(persistentMapOf<Int, RunInfo>()) { acc, value ->
                    acc.put(value.id, value)
                }.stateIn(this)
            )
            log.info("HERE2!")
        }
    }

    routing {
        with (ClicsExporter) {
            route("/clics") {
                setUp(application + handler, contestInfoDeferred, runsDeferred)
            }
        }
        with (PCMSExporter) {
            route("/pcms") {
                setUp(contestInfoDeferred, runsCollectedDeferred)
            }
        }
    }

    log.info("Configuration is done")
}
