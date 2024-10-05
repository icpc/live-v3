package org.icpclive

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.*
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.finalContestState
import org.icpclive.cds.api.ContestInfo
import org.icpclive.cds.api.RunInfo
import org.icpclive.cds.cli.CdsCommandLineOptions
import org.icpclive.cds.util.getLogger
import org.icpclive.server.LoggingOptions
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

abstract class DumpFileCommand(
    name: String,
    val help: String,
    defaultFileName: String,
    outputHelp: String
) : CliktCommand(name = name) {
    abstract fun format(info: ContestInfo, runs: List<RunInfo>): String

    override fun help(context: Context) = help
    override val printHelpOnEmptyArgs = true

    private val cdsOptions by CdsCommandLineOptions()
    private val loggingOptions by LoggingOptions(logfileDefaultPrefix = "converter")
    private val output by option("-o", "--output", help = outputHelp).path().convert {
        if (it.isDirectory()) {
            it.resolve(defaultFileName)
        } else {
            it
        }
    }.required()
        .check({ "Directory ${it.absolute().parent} doesn't exist"}) { it.absolute().parent.isDirectory() }

    open fun Flow<ContestUpdate>.postprocess() = this


    companion object {
        val logger by getLogger()
    }

    override fun run() {
        loggingOptions.setupLogging()
        logger.info { "Would save result to ${output}" }
        val flow = cdsOptions.toFlow()
        val data = runBlocking {
            logger.info { "Waiting till contest become finalized..." }
            val result = flow.postprocess().finalContestState()
            logger.info { "Loaded contest data" }
            result
        }
        val dump = format(
            data.infoAfterEvent!!,
            data.runsAfterEvent.values.toList(),
        )
        output.toFile().printWriter().use {
            it.println(dump)
        }
    }
}