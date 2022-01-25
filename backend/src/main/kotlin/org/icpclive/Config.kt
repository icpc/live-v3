package org.icpclive

import kotlin.Throws
import java.io.IOException
import java.util.Properties
import java.io.FileInputStream
import java.io.File

object Config {
    var configDirectory = "config"
    @JvmStatic
    @Throws(IOException::class)
    fun loadProperties(name: String): Properties {
        val properties = Properties()
        properties.load(FileInputStream(configDirectory + File.separator + name + ".properties"))
        return properties
    }
}