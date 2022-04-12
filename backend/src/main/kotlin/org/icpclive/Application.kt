package org.icpclive

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import org.icpclive.adminapi.configureAdminApiRouting
import org.icpclive.cds.launchEventsLoader
import org.icpclive.config.Config
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.service.EventLoggerService
import org.icpclive.utils.defaultJsonSettings
import org.slf4j.event.Level
import java.io.File
import java.nio.file.Paths
import java.time.Duration

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(DefaultHeaders)
    install(CORS) {
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("*")
        allowMethod(HttpMethod.Delete)
        allowSameOrigin = true
        anyHost()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(AutoHeadResponse)
    install(IgnoreTrailingSlash)
    install(ContentNegotiation) {
        json(defaultJsonSettings())
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    install(StatusPages) {
        exception<Throwable> { call, ex ->
            call.application.environment.log.error("Query ${call.url()} failed with exception", ex)
            throw ex
        }
    }
    environment.config.propertyOrNull("live.configDirectory")?.getString()?.run {
        val configPath = File(this).canonicalPath
        environment.log.info("Using config directory $configPath")
        Config.configDirectory = this
    }
    val mediaPath = Paths.get(Config.configDirectory, environment.config.property("live.mediaDirectory").getString())
    mediaPath.toFile().mkdirs()
    routing {
        static("/static") { resources("static") }
        static("/media") { files(mediaPath.toString()) }
        singlePageApplication {
            useResources = true
            applicationRoute = "admin"
            react("admin")
        }
        singlePageApplication {
            useResources = true
            applicationRoute = "frontend"
            react("frontend")
        }
    }
    configureAdminApiRouting()
    configureOverlayRouting()
    environment.log.info("Current working directory is ${File(".").canonicalPath}")
    launchEventsLoader()
    launch { EventLoggerService().run() }
    // to trigger init
    TickerManager.let {}
    WidgetManager.let {}
}