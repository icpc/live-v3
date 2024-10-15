@file:Suppress("unused")

package org.icpclive

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.export.clics.ClicsExporter
import org.icpclive.export.icpc.IcpcCsvExporter
import org.icpclive.export.pcms.PCMSExporter
import org.icpclive.server.setupDefaultKtorPlugins
import kotlin.system.exitProcess


object MainCommand : CliktCommand(name = "java -jar cds-converter.jar") {
    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true)}
        }
    }
    override val invokeWithoutSubcommand = true
    override val treatUnknownOptionsAsArgs = true
    val unused by argument().multiple()
    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            if (unused.isNotEmpty()) {
                currentContext.terminal.danger("Unknown command ${unused.firstOrNull()}")
                currentContext.terminal.info("")
            }
            throw PrintHelpMessage(currentContext, true)
        }
    }
}

fun main(args: Array<String>): Unit = MainCommand.subcommands(
    PCMSDumpCommand,
    ServerCommand,
    IcpcCSVDumpCommand
).main(args)


@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupDefaultKtorPlugins()

    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        exitProcess(1)
    }

    val loaded = ServerCommand.cdsOptions.toFlow()
        .addComputedData {
            submissionResultsAfterFreeze = !ServerCommand.cdsOptions.freeze
            submissionsAfterEnd = ServerCommand.cdsOptions.upsolving
        }
        .shareIn(this + handler, SharingStarted.Eagerly, Int.MAX_VALUE)

    routing {
        get {
            call.respondText(
                """
                    <html>
                    <body>
                    <a href="/pcms/standings.xml">PCMS xml</a> <br/>
                    <a href="/clics/api/contests/contest">CLICS api root</a> <br/>
                    <a href="/clics/api/contests/contest/event-feed">CLICS event feed</a> <br/>
                    <a href="/icpc/standings.csv">ICPC global csv</a> <br/>
                    </body>
                    </html>
                """.trimIndent(),
                ContentType.Text.Html
            )
        }
        with (IcpcCsvExporter) {
            route("/icpc") {
                setUp(application + handler, loaded)
            }
        }
        with (ClicsExporter) {
            route("/clics") {
                setUp(application + handler, loaded)
            }
        }
        with (PCMSExporter) {
            route("/pcms") {
                setUp(application + handler, loaded)
            }
        }
    }

    log.info("Configuration is done")
}