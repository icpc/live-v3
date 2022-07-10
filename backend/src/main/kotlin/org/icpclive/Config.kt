package org.icpclive

import java.nio.file.Path

object Config {
    lateinit var configDirectory: Path
    lateinit var creds: Map<String, String>
    var allowUnsecureConnections: Boolean = false
}