package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import org.icpclive.server.LoggingOptions
import org.icpclive.server.Publisher
import org.icpclive.server.ServerOptions

object ServerCommand : CliktCommand(name = "server") {
    override val printHelpOnEmptyArgs = true
    override fun help(context: Context) = "Start as http server"
    val cdsOptions by ExtendedCdsCommandLineOptions()
    private val serverOptions by ServerOptions()
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "converter")
    val publisher by Publisher().cooccurring()

    override fun run() {
        loggingOptions.setupLogging()
        serverOptions.start()
    }
}