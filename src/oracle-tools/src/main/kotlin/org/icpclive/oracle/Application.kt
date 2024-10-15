package org.icpclive.oracle

import com.github.ajalt.clikt.core.main
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.server.serverResponseJsonSettings
import org.icpclive.server.setupDefaultKtorPlugins

fun main(args: Array<String>): Unit = Config.main(args)

private fun Application.setupKtorPlugins() {
    setupDefaultKtorPlugins()
    install(ContentNegotiation) { json(serverResponseJsonSettings()) }
}

@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupKtorPlugins()
    Util.initForServer()
    routing {
        singlePageApplication {
            useResources = true
            applicationRoute = "locator"
            react("locator")
        }
        get {
            call.respondRedirect("/locator")
        }

        route("/api") {
            route("/admin") { setupRouting() }
        }
    }
}
