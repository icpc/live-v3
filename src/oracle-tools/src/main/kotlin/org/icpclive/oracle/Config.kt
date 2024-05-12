package org.icpclive.oracle

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import org.icpclive.server.LoggingOptions
import org.icpclive.server.ServerOptions

object Config : CliktCommand(name = "java -jar live-v3.jar", printHelpOnEmptyArgs = false) {
    val configDirectory by option(
        "-c", "--config-directory",
        help = "Path to config directory",
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()

    private val serverOptions by ServerOptions()
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "oracletools")

    val oraclesTxtPath by option("--oracles-txt", help = "Path to oracles.txt")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/oracles.txt") { configDirectory.resolve("oracles.txt") }

    val overlayURL: String by option("-o", "--overlay", "--overlay-url", help = "Main overlay url").default("http://127.0.0.1:8080")

    val authDisabled by option(
        "--no-auth",
        help = "Disable http basic auth in admin"
    ).flag()

    val credsJsonPath by option("--creds", help = "Path to creds.json").path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/creds.json") { configDirectory.resolve("creds.json") }


    override fun run() {
        loggingOptions.setupLogging()
        serverOptions.start()
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) }
        }
    }
}

val config: Config get() = Config
