package org.icpclive

import com.github.ajalt.clikt.core.main
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.icpclive.admin.UsersController
import org.icpclive.admin.configureAdminApiRouting
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.ktor.KtorNetworkSettingsProvider
import org.icpclive.cds.ktor.NetworkSettings
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.util.completeOrThrow
import org.icpclive.cds.util.fileJsonContentFlow
import org.icpclive.data.Controllers
import org.icpclive.data.DataBus
import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.server.*
import org.icpclive.service.KeylogService
import org.icpclive.service.launchServices
import kotlin.system.exitProcess


fun main(args: Array<String>): Unit = Config.main(args)

private fun Application.setupKtorPlugins(userController: UsersController) {
    setupDefaultKtorPlugins()
    install(ContentNegotiation) { json(serverResponseJsonSettings()) }
    install(Authentication) {
        if (Config.authDisabled) {
            val config = object : AuthenticationProvider.Config("admin-api-auth") {}
            register(object : AuthenticationProvider(config) {
                override suspend fun onAuthenticate(context: AuthenticationContext) {
                    context.principal(userController.validateAdminApiCredits("", "")!!)
                }
            })
        } else {
            basic("admin-api-auth") {
                realm = "Access to the '/api/admin' path"
                validate { credentials ->
                    userController.validateAdminApiCredits(
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
    val controllers = Controllers(this)
    setupKtorPlugins(controllers.userController)

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
            route("/admin") { configureAdminApiRouting(controllers) }
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
        fun registerKeylogService(config: CDSSettings) {
            val networkSettings = (config as? KtorNetworkSettingsProvider)?.network ?: NetworkSettings()
            DataBus.keylogService.completeOrThrow(KeylogService(networkSettings))
        }
        val loader = config.cdsSettings
            .toFlow(configObserver = { registerKeylogService(it) })
            .addComputedData()

        val visualConfigFlow = fileJsonContentFlow<JsonObject>(
            config.visualConfigFile,
            noData = JsonObject(emptyMap()),
            json = Json {
                allowComments = true
                allowTrailingComma = true
            }
        ).stateIn(this)

        DataBus.visualConfigFlow.completeOrThrow(visualConfigFlow)
        launchServices(loader, controllers)
    }
}
