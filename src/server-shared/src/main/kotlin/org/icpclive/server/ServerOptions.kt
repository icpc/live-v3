package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.server.netty.EngineMain

public class ServerOptions : OptionGroup("server settings") {
    public val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    public val ktorArgs: List<String> by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    public fun start() {
        EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }
}

