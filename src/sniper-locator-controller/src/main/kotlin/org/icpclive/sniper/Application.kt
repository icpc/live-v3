package org.icpclive.sniper

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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.icpclive.util.defaultJsonSettings
import org.slf4j.event.Level
import java.time.Duration

fun main(args: Array<String>): Unit = Config.main(args)

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
//    environment.log.info("Using config directory ${Config.configDirectory.toAbsolutePath()}")
//    environment.log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")
    setupKtorPlugins()
    routing {
        singlePageApplication {
            useResources = true
            applicationRoute = "admin"
            react("admin")
        }
        get {
            call.respondRedirect("/admin")
        }

        route("/api") {
            post("/move") {
                val text = call.receiveText();
                try {
                    val request = Json.decodeFromString<MoveRequest>(text)
                    SniperMover.moveToTeam(request.sniperID, request.teamID);
                } catch (e: SerializationException) {
                    throw e;
                }
            }
            get("/snipers") {
                val ids = SnipersID(ArrayList());
                for (sniper in Util.snipers) {
                    ids.ids.add(sniper.cameraID + 1);
                }
                call.respond(ids);
            }
            get("/teams") {
                val hostName = Config.overlayURL + "api/admin/teamView/teams";
                val teams = Json.decodeFromString<TeamsResponse>(Util.sendGet(hostName))
                call.respond(teams)
            }
        }
    }
}

//private val logger = getLogger(Application::class)
