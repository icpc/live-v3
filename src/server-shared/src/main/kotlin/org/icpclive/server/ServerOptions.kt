package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int

class ServerOptions : OptionGroup("server settings") {
    private val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    private val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    fun start() {
        io.ktor.server.netty.EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }
}

