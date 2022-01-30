package org.icpclive

import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Config {
    var configDirectory = "config"
    fun loadProperties(name: String): Properties {
        val properties = Properties()
        properties.load(FileInputStream(configDirectory + File.separator + name + ".properties"))
        return properties
    }

    fun loadFile(name: String) =
        String(Files.readAllBytes(Paths.get(configDirectory, name)), StandardCharsets.UTF_8)
}