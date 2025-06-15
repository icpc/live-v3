@file:Suppress("unused")

package org.icpclive.converter

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.mordant.terminal.danger
import com.github.ajalt.mordant.terminal.info
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onStart
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.shareWith
import org.icpclive.converter.commands.*
import org.icpclive.converter.export.Exporter
import org.icpclive.converter.export.Router
import org.icpclive.converter.export.clics.ClicsExporter
import org.icpclive.converter.export.icpc.IcpcCsvExporter
import org.icpclive.converter.export.pcms.PCMSHtmlExporter
import org.icpclive.converter.export.pcms.PCMSXmlExporter
import org.icpclive.converter.export.reactions.ReactionsExporter
import org.icpclive.server.*
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
    PCMSScoreboardDumpCommand,
    ServerCommand,
    IcpcCSVDumpCommand
).main(args)


private val logger by getLogger()


@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupDefaultKtorPlugins()

    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        exitProcess(1)
    }

    ServerCommand.publisher?.let { startPublisher(it) }

    val routers = mutableListOf<Router>()

    val scope = this + handler

    ServerCommand.cdsOptions.toFlow()
        .addComputedData {
            submissionResultsAfterFreeze = !ServerCommand.cdsOptions.freeze
            submissionsAfterEnd = ServerCommand.cdsOptions.upsolving
            autoFinalize = !ServerCommand.cdsOptions.noAutoFinalize
        }
        .calculateScoreboard(OptimismLevel.NORMAL)
        .shareWith(scope) {
            fun Exporter.run() = withSubscription(subscriptionCount) {
                routers += (scope + CoroutineName(this@run::class.simpleName!!)).runOn(it.onStart {
                    logger.info { "Exporter ${this@run::class.simpleName} subscribed to cds data" }
                })
            }
            PCMSXmlExporter.run()
            PCMSHtmlExporter.run()
            IcpcCsvExporter.run()
            ClicsExporter.run()
            ReactionsExporter.run()
        }

    routing {
        install(ContentNegotiation) { json(serverResponseJsonSettings()) }
        get {
            call.respondText(
                """
                    <html>
                    <body>
                    <a href="/pcms/standings.xml">PCMS xml</a> <br/>
                    <a href="/pcms/standings.html">PCMS html</a> <br/>
                    <a href="/clics/api/contests/contest">CLICS api root</a> <br/>
                    <a href="/clics/api/contests/contest/event-feed">CLICS event feed</a> <br/>
                    <a href="/icpc/standings.csv">ICPC global csv</a> <br/>
                    <a href="/reactions">Reaction videos API</a> <br/>
                    </body>
                    </html>
                """.trimIndent(),
                ContentType.Text.Html
            )
        }
        for (router in routers) {
            with(router) {
                setUpRoutes()
            }
        }
    }

    log.info("Configuration is done")
}