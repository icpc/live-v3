package org.icpclive.converter.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import org.icpclive.server.*

object ServerCommand : CliktCommand(name = "server") {
    override val printHelpOnEmptyArgs = true
    override fun help(context: Context) = "Start as http server"
    val cdsOptions by ExtendedCdsCommandLineOptions(defaultAutoFinalize = false)
    private val serverOptions by ServerOptions()
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "converter")
    val publisher by Publisher().cooccurring()
    val mediaDirectory by option("--media-dir", help = "Directory to store media")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/media") { cdsOptions.configDirectory.resolve("media") }

    override fun run() {
        loggingOptions.setupLogging()
        serverOptions.start()
    }
}