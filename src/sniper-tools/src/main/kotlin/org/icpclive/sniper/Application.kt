package org.icpclive.sniper

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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import io.ktor.server.config.*
import kotlinx.coroutines.*
//import org.icpclive.admin.configureAdminApiRouting
//import org.icpclive.data.Controllers
//import org.icpclive.overlay.configureOverlayRouting
import org.icpclive.util.*
//import org.icpclive.cds.getContestDataSource
//import org.icpclive.data.DataBus
//import org.icpclive.service.AdvancedPropertiesService
//import org.icpclive.service.launchServices
import org.slf4j.event.Level
import java.time.Duration
import java.util.*

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
    LocatorController.overlayUrl = environment.config.property("live.overlayUrl").getString()
    setupKtorPlugins()

    routing {
        singlePageApplication {
            useResources = true
            applicationRoute = "admin"
            react("admin")
        }
        route("/api") {
            get("/teams") {
                call.respondText(
                    Util.sendGet("${LocatorController.overlayUrl}/api/admin/teamView/teams"),
                    ContentType.Application.Json
                )
            }
            post("/move") {
                val config = call.receive<MoveSniperConfig>()
                val newPoint = SniperMover.moveToTeam(config.sniperNumber, config.teamId)
                if (newPoint != null) {
                    call.respondText("ok")
                } else {
                    call.respondText("no such team id")
                }
            }
            route("/overlay") {
                post("/show") {
                    try {
                        val config = call.receive<ShowLocatorConfig>()
                        LocatorController.showLocatorWidget(config.sniperNumber, config.teamIds.toSet())
                    } catch (e: Throwable) {
                        println(e)
                    }
                }
                post("/hide") {
                    LocatorController.hideLocatorWidget()
                }
            }
        }
        get {
            call.respondRedirect("/admin/locator", false)
        }
    }
}
