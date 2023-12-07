package org.icpclive.sniper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

object Config : CliktCommand(name = "java -jar live-v3.jar", printHelpOnEmptyArgs = false) {
    val configDirectory by option(
        "-c", "--config-directory",
        help = "Path to config directory",
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()

    val port: Int by option("-p", "--port", help = "Port to listen").int().default(8083)

    val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    val snipersTxtPath by option("--snipers-txt", help = "Path to snipers.txt")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/snipers.txt") { configDirectory.resolve("snipers.txt") }

    val coordinatesTxtPath: String = configDirectory.toAbsolutePath().toString() + "/coordinates";

    val overlayURL = "127.0.0.1:8080"

    override fun run() {
        println("Fdsfdsfds")
        io.ktor.server.netty.EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) }
        }
    }

}

val config: Config get() = Config
