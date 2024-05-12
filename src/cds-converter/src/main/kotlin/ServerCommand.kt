package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.server.LoggingOptions
import org.icpclive.server.ServerOptions

object ServerCommand : CliktCommand(name = "server", help = "Start as http server", printHelpOnEmptyArgs = true) {
    val cdsOptions by CdsCommandLineOptions()
    private val serverOptions by ServerOptions()
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "converter")

    override fun run() {
        loggingOptions.setupLogging()
        serverOptions.start()
    }
}