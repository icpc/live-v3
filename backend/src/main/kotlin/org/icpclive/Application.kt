package org.icpclive

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.admin.validateAdminApiCredits
import org.icpclive.cds.launchEventsLoader
import org.icpclive.config.Config
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.service.EventLoggerService
import org.icpclive.utils.defaultJsonSettings
import org.slf4j.event.Level
import java.nio.file.Paths
import java.time.Duration
import kotlin.io.path.exists

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

private fun Application.setupKtorPlugins() {
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
    install(Authentication) {
        basic("admin-api-auth") {
            realm = "Access to the '/api/admin' path"
            validate { credentials -> validateAdminApiCredits(credentials.name, credentials.password) }
        }
    }
}

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupKtorPlugins()
    environment.log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")
    run {
        val configDir = environment.config.propertyOrNull("live.configDirectory")
            ?.getString()
            ?: throw IllegalStateException("Config directory should be set")
        val configPath = Paths.get(configDir).toAbsolutePath()
        if (!configPath.exists()) throw IllegalStateException("Config directory $configPath does not exist")
        environment.log.info("Using config directory $configPath")
        Config.configDirectory = Paths.get(configDir)
    }

    val mediaPath = Config.configDirectory.resolve(environment.config.property("live.mediaDirectory").getString())
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
            applicationRoute = "overlay"
            react("overlay")
        }
        route("/api") {
            route("/admin") { configureAdminApiRouting() }
            route("/overlay") { configureOverlayRouting() }
        }
    }
    launchEventsLoader()
    launch { EventLoggerService().run() }
    // to trigger init
    TickerManager.let {}
    WidgetManager.let {}
}
