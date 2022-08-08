package org.icpclive

import io.ktor.server.application.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class Config(environment: ApplicationEnvironment) {
    val configDirectory: Path
    val presetsDirectory: Path
    val mediaDirectory: Path
    val creds: Map<String, String>
    val allowUnsecureConnections: Boolean

    init {
        val configDir = environment.config.propertyOrNull("live.configDirectory")
            ?.getString()
            ?: throw IllegalStateException("Config directory should be set")

        val configPath = Paths.get(configDir).toAbsolutePath()
        if (!configPath.exists()) throw IllegalStateException("Config directory $configPath does not exist")
        environment.log.info("Using config directory $configPath")

        configDirectory = Paths.get(configDir)
        presetsDirectory = configDirectory.resolve(environment.config.property("live.presetsDirectory").getString())
        mediaDirectory = configDirectory.resolve(environment.config.property("live.mediaDirectory").getString())

        presetsDirectory.toFile().mkdirs()
        mediaDirectory.toFile().mkdirs()

        creds = environment.config.propertyOrNull("live.credsFile")?.let {
            Json.decodeFromStream(File(it.getString()).inputStream())
        } ?: emptyMap()
        allowUnsecureConnections =
            environment.config.propertyOrNull("live.allowUnsecureConnections")?.getString() == "true"
    }
}

lateinit var config: Config
