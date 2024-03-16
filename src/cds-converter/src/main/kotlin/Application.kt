@file:Suppress("unused")

package org.icpclive

import ClicsExporter
import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.icpclive.cds.tunning.AdvancedProperties
import org.icpclive.cds.tunning.TeamInfoOverride
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.cds.api.*
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.export.icpc.csv.IcpcCsvExporter
import org.icpclive.util.*
import org.icpclive.org.icpclive.export.pcms.PCMSExporter
import org.slf4j.event.Level
import java.time.Duration
import kotlin.io.path.*
import kotlin.system.exitProcess

abstract class DumpFileCommand(
    name: String,
    help: String,
    defaultFileName: String,
    outputHelp: String
) : CliktCommand(name = name, help = help, printHelpOnEmptyArgs = true) {
    abstract fun format(info: ContestInfo, runs: List<RunInfo>): String

    val cdsOptions by CdsCommandLineOptions()
    private val output by option("-o", "--output", help = outputHelp).path().convert {
        if (it.isDirectory()) {
            it.resolve(defaultFileName)
        } else {
            it
        }
    }.required()
        .check({ "Directory ${it.absolute().parent} doesn't exist"}) { it.absolute().parent.isDirectory() }

    open fun Flow<ContestUpdate>.postprocess() = this


    override fun run() {
        val logger = getLogger(DumpFileCommand::class)
        logger.info("Would save result to ${output}")
        val flow = cdsOptions.toFlow(logger)
        val data = runBlocking {
            logger.info("Waiting till contest become finalized...")
            val result = flow.postprocess().finalContestState()
            logger.info("Loaded contest data")
            result
        }
        val dump = format(
            data.infoAfterEvent!!,
            data.runs.values.toList(),
        )
        output.toFile().printWriter().use {
            it.println(dump)
        }
    }
}

object PCMSDumpCommand : DumpFileCommand(
    name = "pcms",
    help = "Dump pcms xml",
    outputHelp = "Path to new xml file",
    defaultFileName = "standings.xml"
) {
    override fun format(info: ContestInfo, runs: List<RunInfo>) = PCMSExporter.format(info, runs)
}

object IcpcCSVDumpCommand : DumpFileCommand(
    name = "icpc-csv",
    help = "Dump csv for icpc.global",
    outputHelp = "Path to new csv file",
    defaultFileName = "standings.csv"
) {
    val teamsMapping by option("--teams-map", help = "mapping from cds team id to icpc team id")
        .file(canBeFile = true, canBeDir = false, mustExist = true)

    override fun format(info: ContestInfo, runs: List<RunInfo>) = IcpcCsvExporter.format(info, runs)
    override fun Flow<ContestUpdate>.postprocess(): Flow<ContestUpdate> {
        val mappingFile = teamsMapping
        if (mappingFile == null) {
            return this
        } else {
            val parser = CSVParser(mappingFile.inputStream().reader(), CSVFormat.TDF)
            val map = parser.records.associate {
                TeamId(it[1]!!) to it[0]!!
            }
            val advanced = AdvancedProperties(
                teamOverrides = map.mapValues {
                    TeamInfoOverride(
                        customFields = mapOf("icpc_id" to it.value)
                    )
                }
            )
            return applyAdvancedProperties(flow { emit(advanced) })
        }
    }
}


object ServerCommand : CliktCommand(name = "server", help = "Start as http server", printHelpOnEmptyArgs = true) {
    val cdsOptions by CdsCommandLineOptions()
    val port: Int by option("-p", "--port", help = "Port to listen").int().default(8080)
    val ktorArgs by option("--ktor-arg", help = "Arguments to forward to ktor server").multiple()

    override fun run() {
        io.ktor.server.netty.EngineMain.main((listOf("-port=$port") + ktorArgs).toTypedArray())
    }
}

object MainCommand : CliktCommand(name = "java -jar cds-converter.jar", invokeWithoutSubcommand = true, treatUnknownOptionsAsArgs = true) {
    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showRequiredTag = true, showDefaultValues = true)}
        }
    }
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


private fun Application.setupKtorPlugins() {
    install(DefaultHeaders)
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(StatusPages) {
        exception<Throwable> { call, ex ->
            call.application.environment.log.error("Query ${call.url()} failed with exception", ex)
            throw ex
        }
    }
    install(AutoHeadResponse)
    install(IgnoreTrailingSlash)
    install(ContentNegotiation) { json(defaultJsonSettings()) }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    install(CORS) {
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("*")
        allowMethod(HttpMethod.Delete)
        allowSameOrigin = true
        anyHost()
    }
}

@Suppress("unused") // application.yaml references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    setupKtorPlugins()

    val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
        environment.log.error("Uncaught exception in coroutine context $coroutineContext", throwable)
        exitProcess(1)
    }

    val loaded = ServerCommand.cdsOptions.toFlow(environment.log)
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