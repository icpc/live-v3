package org.icpclive.server

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.port
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.icpclive.cds.util.getLogger
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.name
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

class Publisher : OptionGroup("publisher") {
    val interval by option("--publisher-interval", help = "Interval in seconds for publishing")
        .int()
        .convert { it.seconds }
        .required()
    val paths by option("--publish", help = "Publish url to directory", metavar = "{url}:{path}").convert {
        val (a, b) = it.split(":")
        a to Path(b).absolute()
    }.multiple(required = true).validate {
        for ((_, p) in it) {
            require(p.parent.exists()) { "Directory ${p.parent} does not exist" }
        }
    }
}

private val log by getLogger()

fun Application.startPublisher(config: Publisher) {
    log.info { "Stating publiser with interval ${config.interval} seconds and paths ${config.paths}" }
    launch(CoroutineName("publisher")) {
        while (true) {
            delay(config.interval)
            for ((q, p) in config.paths) {
                val client = HttpClient {
                    defaultRequest {
                        host = "0.0.0.0"
                        port = this@startPublisher.environment.config.port
                    }
                }
                val data = client.get(q).bodyAsText()
                val tmpP = p.resolveSibling(p.name + ".tmp")
                tmpP.writeText(data)
                tmpP.moveTo(p, overwrite = true)
            }
        }
    }
}