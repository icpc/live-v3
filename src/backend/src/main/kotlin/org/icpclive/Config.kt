package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.server.LoggingOptions
import org.icpclive.server.ServerOptions
import org.icpclive.util.FlowLogAppender

object Config : CliktCommand(name = "java -jar live-v3.jar") {
    override val printHelpOnEmptyArgs = true
    val cdsSettings by CdsCommandLineOptions()
    private val serverSettings by ServerOptions()
    private val loggingSettings by LoggingOptions(logfileDefaultPrefix = "icpclive")

    val authDisabled by option(
        "--no-auth",
        help = "Disable http basic auth in admin"
    ).flag()

    val presetsDirectory by cdsSettings.configSubDirWithDefault(
        optionName = "--presets-dir",
        help = "Directory to store presets",
        defaultName = "presets",
        mustExist = false
    )

    val mediaDirectory by cdsSettings.configSubDirWithDefault(
        "--media-dir",
        help = "Directory to store media",
        defaultName = "media",
        mustExist = false
    )

    val usersFile by cdsSettings.configFileWithDefault(
        optionName = "--users-file",
        help = "Storage of users",
        defaultName = "users.json",
        mustExist = false,
    )

    val analyticsTemplatesFile by option(
        "--analytics-template",
        help = "File with localization of analytics messages"
    ).path(canBeFile = true, canBeDir = false, mustExist = true)

    val visualConfigFile by cdsSettings.configFileWithDefault(
        optionName = "--visual-config",
        help = "File with frontend configuration",
        defaultName = "visual-config.json",
    )

    override fun run() {
        loggingSettings.setupLogging(extraLoggers = listOf(::FlowLogAppender))
        presetsDirectory.toFile().mkdirs()
        mediaDirectory.toFile().mkdirs()
        serverSettings.start()
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) }
        }
    }
}

val config: Config get() = Config
