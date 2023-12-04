package org.icpclive.sniper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

object Config : CliktCommand(name = "java -jar live-v3.jar", printHelpOnEmptyArgs = false) {
//    val configDirectory by option(
//        "-c", "--config-directory",
//        help = "Path to config directory",
//    ).path(mustExist = true, canBeFile = false, canBeDir = true).default(Path.of("."))

    val port: Int by option("-p", "--port", help = "Port to listen").int().default(8083)

    val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

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
