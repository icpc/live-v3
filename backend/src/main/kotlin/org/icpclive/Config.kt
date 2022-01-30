package org.icpclive

import kotlin.Throws
import java.io.IOException
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

object Config {
    var configDirectory = "config"
    fun loadProperties(name: String): Properties {
        val properties = Properties()
        properties.load(FileInputStream(configDirectory + File.separator + name + ".properties"))
        return properties
    }
    fun loadFile(name:String) =
        String(Files.readAllBytes(Paths.get(configDirectory, name)), StandardCharsets.UTF_8)
}