package org.icpclive

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object Config {
    lateinit var configDirectory: Path
    lateinit var creds: Map<String, String>
    var allowUnsecureConnections: Boolean = false
}