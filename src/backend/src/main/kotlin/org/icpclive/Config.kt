package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.LocationRectangle

object Config : CliktCommand(name = "java -jar live-v3.jar", printHelpOnEmptyArgs = true) {
    val configDirectory by option(
        "-c", "--config-directory",
        help = "Path to config directory"
    ).path(mustExist = true, canBeFile = false, canBeDir = true).required()

    val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    val authDisabled by option(
        "--no-auth",
        help = "Disable http basic auth in admin"
    ).flag()


    val creds by option(
        "--creds",
        help = "Path to file with credentials"
    ).path(mustExist = true, canBeFile = true, canBeDir = false)
        .convert { path ->
            path.toFile().inputStream().use { Json.decodeFromStream<Map<String, String>>(it) }
        }.default(emptyMap(), "none")

    val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    val advancedJsonPath by option("--advanced-json", help = "Path to advanced.json")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .defaultLazy("configDirectory/advanced.json") { configDirectory.resolve("advanced.json") }

    val presetsDirectory by option("--presets-dir", help = "Directory to store presets")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/presets") { configDirectory.resolve("presets") }
    val mediaDirectory by option("--media-dir", help = "Directory to store media")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/media") { configDirectory.resolve("media") }

    val widgetPositions by option(
        "--widget-positions",
        help = "File with custom widget positions"
    ).path(canBeDir = false, mustExist = true, canBeFile = true).convert { path->
            path.toFile().inputStream().use { Json.decodeFromStream<Map<String, LocationRectangle>>(it) }
        }.default(emptyMap(), "none")

    val analyticsTemplatesFile by option(
        "--analytics-template",
        help = "File with localization of analytics messages"
    ).path(canBeFile = true, canBeDir = false, mustExist = true)

    override fun run() {
        presetsDirectory.toFile().mkdirs()
        mediaDirectory.toFile().mkdirs()
        io.ktor.server.netty.EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }

    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true) }
        }
    }

}

val config: Config get() = Config