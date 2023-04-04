package org.icpclive

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
import kotlinx.coroutines.*
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.data.Controllers
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.util.*
import org.icpclive.cds.getContestDataSource
import org.icpclive.data.DataBus
import org.icpclive.service.AdvancedPropertiesService
import org.icpclive.service.launchServices
import org.slf4j.event.Level
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
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
    install(Authentication) {
        if (config.authDisabled) {
            val config = object : AuthenticationProvider.Config("admin-api-auth") {}
            register(object : AuthenticationProvider(config) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(Controllers.userController.validateAdminApiCredits("", "")!!)
                }
            })
        } else {
            basic("admin-api-auth") {
                realm = "Access to the '/api/admin' path"
                validate { credentials ->
                    Controllers.userController.validateAdminApiCredits(
                        credentials.name,
                        credentials.password
                    )
                }
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

@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    config = Config(environment)
    setupKtorPlugins()

    routing {
        static("/static") { resources("static") }
        static("/media") { files(config.mediaDirectory.toString()) }
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
    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        // TODO: understand why normal exception propagation doesn't work
        exitProcess(1)
    }
    val path = config.configDirectory.resolve("events.properties")
    if (!Files.exists(path)) throw FileNotFoundException("events.properties not found in ${config.configDirectory}")
    val properties = Properties()
    FileInputStream(path.toString()).use { properties.load(it) }
    val loader = getContestDataSource(
        properties,
        config.creds,
        calculateFTS = true,
        calculateDifference = true,
        removeFrozenResults = true,
        advancedPropertiesDeferred = DataBus.advancedPropertiesFlow
    )

    launch(handler) {
        launch { AdvancedPropertiesService().run(DataBus.advancedPropertiesFlow) }
        launchServices(loader)
    }
}
