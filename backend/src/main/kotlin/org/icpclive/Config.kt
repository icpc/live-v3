package org.icpclive

import io.ktor.server.application.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.LocationRectangle
import org.icpclive.cds.common.setAllowUnsecureConnections
import org.icpclive.util.getCredentials
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.exists

class Config(environment: ApplicationEnvironment) {
    private fun ApplicationConfig.stringOrNull(name: String) = propertyOrNull(name)?.getString()
    private fun ApplicationConfig.string(name: String) = property(name).getString()
    private fun ApplicationConfig.bool(name: String) = stringOrNull(name) == "true"

    val configDirectory: Path = environment.config.stringOrNull("live.configDirectory")
        ?.let { Paths.get(it).toAbsolutePath() }
        ?.also {
            if (!it.exists()) throw IllegalStateException("Config directory $it does not exist")
            environment.log.info("Using config directory $it")
            environment.log.info("Current working directory is ${Paths.get("").toAbsolutePath()}")
        } ?: throw IllegalStateException("Config directory should be set")

    private fun ApplicationConfig.directory(name: String) = configDirectory.resolve(string(name)).also { it.toFile().mkdirs() }

    val presetsDirectory: Path = environment.config.directory("live.presetsDirectory")
    val mediaDirectory: Path = environment.config.directory("live.mediaDirectory")
    val creds: Map<String, String> = environment.config.stringOrNull("live.credsFile")?.let {
        Json.decodeFromStream(File(it).inputStream())
    } ?: emptyMap()
    val widgetPositions: Map<String, LocationRectangle> = environment.config.stringOrNull("live.widgetPositionsFile")?.let {
        Json.decodeFromStream(File(it).inputStream())
    } ?: emptyMap()
    val allowUnsecureConnections = environment.config.bool("live.allowUnsecureConnections").also {
        setAllowUnsecureConnections(it)
    }
    val authDisabled = environment.config.bool("auth.disabled")
}

lateinit var config: Config

fun Properties.getCredentials(key: String) = getCredentials(key, config.creds)