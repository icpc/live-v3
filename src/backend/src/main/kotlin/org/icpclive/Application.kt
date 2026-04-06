package org.icpclive

import com.github.ajalt.clikt.core.main
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.util.completeOrThrow
import org.icpclive.cds.util.fileJsonContentFlow
import org.icpclive.data.Controllers
import org.icpclive.data.DataBus
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.server.UsefulLink
import org.icpclive.server.configureMainPageRouting
import org.icpclive.server.serverResponseJsonSettings
import org.icpclive.server.setupDefaultKtorPlugins
import org.icpclive.service.launchServices
import kotlin.system.exitProcess


fun main(args: Array<String>): Unit = Config.main(args)

private fun Application.setupKtorPlugins() {
    setupDefaultKtorPlugins()
    install(ContentNegotiation) { json(serverResponseJsonSettings()) }
    install(Authentication) {
        if (Config.authDisabled) {
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
}

@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupKtorPlugins()

    routing {
        staticFiles("/media", Config.mediaDirectory.toFile())
        route("/") {
            install(ConditionalHeaders)
            staticResources("/schemas", "schemas")
            singlePageApplication {
                useResources = true
                applicationRoute = "admin"
                react("admin-overlay")
            }
            singlePageApplication {
                useResources = true
                applicationRoute = "overlay"
                react("overlay")
            }
            get {
                call.respondRedirect("/admin")
            }
        }
        route("/api") {
            route("/admin") { configureAdminApiRouting() }
            route("/overlay") { configureOverlayRouting() }
        }
        configureMainPageRouting(
            listOf(
                UsefulLink("/admin/controls", "/admin/controls"),
                UsefulLink("/overlay?noStatus", "/overlay?noStatus"),
                UsefulLink("/api/admin/advancedJsonPreview?fields=all", "/api/admin/advancedJsonPreview?fields=all"),
                UsefulLink("https://github.com/icpc/live-v3", "https://github.com/icpc/live-v3")
            )
        )
    }
    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        // TODO: understand why normal exception propagation doesn't work
        exitProcess(1)
    }

    launch(handler) {
        val loader = config.cdsSettings
            .toFlow()
            .addComputedData()

        val visualConfigFlow = fileJsonContentFlow<JsonObject>(
            config.visualConfigFile,
            noData = JsonObject(emptyMap())
        ).stateIn(this)

        DataBus.visualConfigFlow.completeOrThrow(visualConfigFlow)

        launchServices(loader)
    }
}
