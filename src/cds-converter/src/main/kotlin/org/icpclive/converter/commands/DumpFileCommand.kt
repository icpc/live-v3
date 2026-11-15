package org.icpclive.converter.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.adapters.finalContestState
import org.icpclive.cds.adapters.finalContestStateWithScoreboard
import org.icpclive.cds.api.ContestStatus
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.getLogger
import org.icpclive.server.LoggingOptions
import java.io.File
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory
import kotlin.time.Duration.Companion.seconds

abstract class DumpFileCommand(
    name: String,
    val help: String,
    defaultFileName: String,
    outputHelp: String,
) : CliktCommand(name = name) {
    override fun help(context: Context) = help
    override val printHelpOnEmptyArgs = true

    protected val cdsOptions by ExtendedCdsCommandLineOptions(defaultAutoFinalize = true)
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "converter")
    private val output by option("-o", "--output", help = outputHelp).path().convert {
        if (it.isDirectory()) {
            it.resolve(defaultFileName)
        } else {
            it
        }
    }.required()
        .check({ "Directory ${it.absolute().parent} doesn't exist"}) { it.absolute().parent.isDirectory() }


    companion object {
        val logger by getLogger()
    }

    abstract fun create(file: File, flow: Flow<ContestUpdate>)

    override fun run() {
        loggingOptions.setupLogging()
        logger.info { "Would save result to ${output}" }
        logger.info { "Settings: freeze=${cdsOptions.freeze}, upsolving=${cdsOptions.upsolving}, autoFinalize=${cdsOptions.autoFinalize}" }
        val flow = cdsOptions
            .toFlow()
            .addComputedData {
                submissionResultsAfterFreeze = !cdsOptions.freeze
                submissionsAfterEnd = cdsOptions.upsolving
                autoFinalize = cdsOptions.autoFinalize
            }
        create(output.toFile(), flow)
    }
}


abstract class DumpTextFileCommand(
    name: String,
    help: String,
    defaultFileName: String,
    outputHelp: String,
) : DumpFileCommand(name, help, defaultFileName, outputHelp) {
    abstract fun format(data: ContestStateWithScoreboard): String
    @OptIn(FlowPreview::class)
    override fun create(file: File, flow: Flow<ContestUpdate>) {
        val data = runBlocking {
            val waitProgress = MutableStateFlow<Pair<ContestStatus, Int>?>(null)
            val logJob = launch {
                waitProgress.filterNotNull().debounce(1.seconds).collect { (status, count) ->
                    logger.info { "Waiting for contest to become Finalized. Current status is ${status}. $count runs are still in progress." }
                }
            }
            val result = try {
                 flow.finalContestStateWithScoreboard { status, count ->
                    if (status != null) {
                        waitProgress.value = status to count
                    }
                }
            } finally {
                logJob.cancel()
            }
            logger.info { "Loaded contest data" }
            result
        }
        val dump = format(data)
        file.printWriter().use {
            it.println(dump)
        }
    }
}