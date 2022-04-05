package org.icpclive

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.http.*
import io.ktor.websocket.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
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
import java.time.Duration

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    install(DefaultHeaders)
    install(CORS) {
        header(HttpHeaders.ContentType)
        header(HttpHeaders.Authorization)
        header("*")
        method(HttpMethod.Delete)
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
    routing {
        static("/static") { resources("static") }
    }
    configureAdminApiRouting()
    configureOverlayRouting()
    environment.log.info("Current working directory is ${File(".").canonicalPath}")
    environment.config.propertyOrNull("live.configDirectory")?.getString()?.run {
        val configPath = File(this).canonicalPath
        environment.log.info("Using config directory $configPath")
        Config.configDirectory = this
    }
    launchEventsLoader()
    launch { EventLoggerService().run() }
    // to trigger init
    TickerManager.let{}
    WidgetManager.let{}
}