package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.application.Application
import io.ktor.server.application.port
import kotlinx.coroutines.*
import org.icpclive.cds.util.getLogger
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds

class Publisher : OptionGroup("publisher") {
    val interval by option("--publisher-interval", help = "Interval in seconds for publishing")
        .int()
        .convert { it.seconds }
        .required()
    val paths by option("--publish", help = "Publish url to directory", metavar = "{url}:{path}").convert {
        val (a, b) = it.split(":")
        a to Path(b).absolute()
    }.multiple(required = true).validate {
        for ((_, p) in it) {
            require(p.parent.exists()) { "Directory ${p.parent} does not exist" }
        }
    }
    val command by option("--publish-command", help = "Execute a callback command after publish is done.")
}

private val log by getLogger()

fun Application.startPublisher(config: Publisher) {
    log.info { "Stating publisher with interval ${config.interval} seconds and paths ${config.paths}" }


    launch(CoroutineName("publisher")) {
        while (true) {
            delay(config.interval)
            for ((q, p) in config.paths) {
                val client = HttpClient {
                    defaultRequest {
                        host = "0.0.0.0"
                        port = this@startPublisher.environment.config.port
                    }
                }
                val data = client.get(q).bodyAsText()
                val tmpP = p.resolveSibling(p.name + ".tmp")
                tmpP.writeText(data)
                tmpP.moveTo(p, overwrite = true)
            }
            config.command?.let { command ->
                withContext(Dispatchers.IO) {
                    try {
                        val commandList = command.split(" ")
                        log.info { "Executing command: ${commandList.joinToString(" ")}" }
                        val processBuilder = ProcessBuilder(commandList)
                        val process = processBuilder.start()
                        val exitCode = process.waitFor()
                        val stdOutput = process.inputStream.bufferedReader().readText()
                        if (exitCode != 0) {
                            val errorOutput = process.errorStream.bufferedReader().readText()
                            log.error { "Command execution failed with exit code $exitCode. Error output: $errorOutput" }
                        } else {
                            log.info { "Command executed successfully. Output: $stdOutput" }
                        }
                    } catch (e: Exception) {
                        log.error(e) { "Failed to execute command" }
                    }
                }
            }
        }
    }
}
