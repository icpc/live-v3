package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.LocationRectangle
import org.icpclive.api.defaultWidgetPositions
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.server.LoggingOptions
import org.icpclive.server.ServerOptions
import org.icpclive.util.FlowLogger

object Config : CliktCommand(name = "java -jar live-v3.jar") {
    override val printHelpOnEmptyArgs = true
    val cdsSettings by CdsCommandLineOptions()
    private val serverSettings by ServerOptions()
    private val loggingSettings by LoggingOptions(logfileDefaultPrefix = "icpclive")

    val authDisabled by option(
        "--no-auth",
        help = "Disable http basic auth in admin"
    ).flag()

    val presetsDirectory by option("--presets-dir", help = "Directory to store presets")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/presets") { cdsSettings.configDirectory.resolve("presets") }
    val mediaDirectory by option("--media-dir", help = "Directory to store media")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/media") { cdsSettings.configDirectory.resolve("media") }
    val usersFile by option("--users-file", help = "Storage of users")
        .path(canBeDir = false, canBeFile = true)
        .defaultLazy("configDirectory/users.json") { cdsSettings.configDirectory.resolve("users.json") }

    val widgetPositions by option(
        "--widget-positions",
        help = "File with custom widget positions"
    ).path(canBeDir = false, mustExist = true, canBeFile = true).convert { path ->
        path.toFile().inputStream().use { Json.decodeFromStream<Map<String, LocationRectangle>>(it) }
    }.default(emptyMap(), "none")

    val analyticsTemplatesFile by option(
        "--analytics-template",
        help = "File with localization of analytics messages"
    ).path(canBeFile = true, canBeDir = false, mustExist = true)

    val visualConfigFile by option(
        "--visual-config",
        help = "File with localization of analytics messages"
    ).path(canBeFile = true, canBeDir = false, mustExist = true)


    override fun run() {
        loggingSettings.setupLogging(extraLoggers = listOf(::FlowLogger))
        defaultWidgetPositions = widgetPositions
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
