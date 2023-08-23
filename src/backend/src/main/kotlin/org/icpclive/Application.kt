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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.api.tunning.AdvancedProperties
import org.icpclive.cds.adapters.*
import org.icpclive.cds.settings.parseFileToCdsSettings
import org.icpclive.data.Controllers
import org.icpclive.data.DataBus
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.service.launchServices
import org.icpclive.util.completeOrThrow
import org.icpclive.util.defaultJsonSettings
import org.icpclive.util.fileJsonContentFlow
import org.slf4j.event.Level
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
        staticFiles("/media", config.mediaDirectory.toFile())
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
    val path =
        config.configDirectory.resolve("events.properties")
            .takeIf { it.exists() }
            ?.also { environment.log.warn("Using events.properties is deprecated, use settings.json instead.") }
            ?: config.configDirectory.resolve("settings.json")

    launch(handler) {
        val advancedJsonPath = config.configDirectory.resolve("advanced.json")
        val advancedPropertiesFlow = fileJsonContentFlow<AdvancedProperties>(advancedJsonPath, environment.log)
            .stateIn(this, SharingStarted.Eagerly, AdvancedProperties())
        DataBus.advancedPropertiesFlow.completeOrThrow(advancedPropertiesFlow)

        val loader = parseFileToCdsSettings(path)
            .toFlow(config.creds)
            .applyAdvancedProperties(advancedPropertiesFlow)
            .contestState()
            .filterUseless()
            .removeFrozenSubmissions()
            .processHiddenTeamsAndGroups()
            .calculateScoreDifferences()
            .addFirstToSolves()


        launchServices(loader)
    }
}
