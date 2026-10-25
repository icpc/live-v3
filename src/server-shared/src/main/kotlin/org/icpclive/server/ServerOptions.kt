package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.icpclive.cds.util.getLogger

public class ServerOptions : OptionGroup("server settings") {
    public val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    public val ktorArgs: List<String> by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    private val reloadRequests = Channel<Job>(Channel.CONFLATED)

    public fun start(): Unit = runBlocking {
        val server = EngineMain.createServer((listOf("-port=$port") + ktorArgs).toTypedArray())
        val reloader = launch(Dispatchers.IO + CoroutineName("app-reloader")) {
            reloadRequests.consumeEach {
                it.join()
                log.info { "Reloading application" }
                try {
                    server.reload()
                    log.info { "Application reloaded" }
                } catch (e: Throwable) {
                    currentCoroutineContext().ensureActive()
                    log.error(e) { "Failed to reload application, keeping the running one" }
                }
            }
        }
        server.startSuspend(wait = true)
        reloader.cancel()
    }

    // [job] is the job the requested reload. It should be finished before reload happens.
    public fun requestReload(job: Job) {
        val _ = reloadRequests.trySend(job)
    }

    private companion object {
        val log by getLogger()
    }
}
