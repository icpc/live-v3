package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.util.getLogger

public class ServerOptions : OptionGroup("server settings") {
    public val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    public val ktorArgs: List<String> by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    // Conflated, so that a burst of requests collapses into a single rebuild.
    private val reloadRequests = Channel<Unit>(Channel.CONFLATED)
    private var started = false

    public fun start(): Unit = runBlocking {
        val server = EngineMain.createServer((listOf("-port=$port") + ktorArgs).toTypedArray())
        started = true
        // A sibling of the server and a child of main, deliberately not in the application's own
        // scope: this coroutine has to outlive the very application it disposes.
        val reloader = launch(Dispatchers.IO + CoroutineName("app-reloader")) {
            for (request in reloadRequests) {
                log.info { "Reloading application" }
                runCatching { server.reload() }
                    .onSuccess { log.info { "Application reloaded" } }
                    .onFailure { log.error(it) { "Failed to reload application, keeping the running one" } }
            }
        }
        server.startSuspend(wait = true)
        reloader.cancel()
    }

    /**
     * Rebuilds the ktor application in place: a new [io.ktor.server.application.Application] is
     * created and the old one is disposed, while the engine keeps the listening socket open.
     *
     * Deliberately fire-and-forget. The caller is normally a coroutine of the very application
     * that is about to be disposed, so awaiting the reload here would deadlock waiting for
     * itself to finish.
     */
    public fun requestReload() {
        check(started) { "Server is not started" }
        reloadRequests.trySend(Unit)
    }

    private companion object {
        val log by getLogger()
    }
}
