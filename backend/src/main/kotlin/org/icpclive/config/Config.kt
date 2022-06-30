package org.icpclive.config

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object Config {
    lateinit var configDirectory: Path
    lateinit var creds: Map<String, String>
    fun loadProperties(name: String) =
        loadPropertiesIfExists(name) ?: throw FileNotFoundException("$name.properties not found in $configDirectory")

    fun loadPropertiesIfExists(name: String): Properties? {
        val path = configDirectory.resolve("$name.properties")
        if (!Files.exists(path)) return null
        val properties = Properties()
        FileInputStream(path.toString()).use { properties.load(it) }
        return properties
    }

    fun loadFile(name: String) =
        String(Files.readAllBytes(configDirectory.resolve(name)), StandardCharsets.UTF_8)

    fun loadFileIfExists(name: String) =
        if (Files.exists(configDirectory.resolve(name)))
            loadFile(name)
        else
            null

    var allowUnsecureConnections: Boolean = false
}