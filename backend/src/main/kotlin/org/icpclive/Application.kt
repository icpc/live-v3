package org.icpclive

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.http.content.*
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.admin.createFakeUser
import org.icpclive.admin.validateAdminApiCredits
import org.icpclive.cds.launchContestDataSource
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
import kotlin.io.path.exists

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
    install(Authentication) {
        if (this@setupKtorPlugins.environment.config.propertyOrNull("auth.disabled")?.getString() == "true") {
            val config = object : AuthenticationProvider.Config("admin-api-auth") {}
            register(object : AuthenticationProvider(config) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(createFakeUser())
                }
            })
        } else {
            basic("admin-api-auth") {
                realm = "Access to the '/api/admin' path"
                validate { credentials -> validateAdminApiCredits(credentials.name, credentials.password) }
            }
        }
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
        Config.creds = environment.config.propertyOrNull("live.credsFile")?.let {
            Json.decodeFromStream(File(it.getString()).inputStream())
        } ?: emptyMap()
        Config.allowUnsecureConnections = environment.config.propertyOrNull("live.allowUnsecureConnections")?.getString() == "true"
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
    launchContestDataSource()
    launch { EventLoggerService().run() }
    // to trigger init
    TickerManager.let {}
    WidgetManager.let {}
}
