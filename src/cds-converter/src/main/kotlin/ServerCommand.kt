package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import org.icpclive.cds.cli.CdsCommandLineOptions

object ServerCommand : CliktCommand(name = "server", help = "Start as http server", printHelpOnEmptyArgs = true) {
    val cdsOptions by CdsCommandLineOptions()
    val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    override fun run() {
        io.ktor.server.netty.EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }
}