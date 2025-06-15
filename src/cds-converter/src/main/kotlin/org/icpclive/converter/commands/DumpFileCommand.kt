package org.icpclive.converter.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.adapters.addComputedData
import org.icpclive.cds.adapters.finalContestStateWithScoreboard
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.util.getLogger
import org.icpclive.server.LoggingOptions
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

abstract class DumpFileCommand(
    name: String,
    val help: String,
    defaultFileName: String,
    outputHelp: String,
) : CliktCommand(name = name) {
    abstract fun format(data: ContestStateWithScoreboard): String

    override fun help(context: Context) = help
    override val printHelpOnEmptyArgs = true

    private val cdsOptions by ExtendedCdsCommandLineOptions()
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

    override fun run() {
        loggingOptions.setupLogging()
        logger.info { "Would save result to ${output}" }
        val flow = cdsOptions
            .toFlow()
            .addComputedData {
                submissionResultsAfterFreeze = !cdsOptions.freeze
                submissionsAfterEnd = cdsOptions.upsolving
            }
        val data = runBlocking {
            logger.info { "Waiting till contest become finalized..." }
            val result = flow.finalContestStateWithScoreboard()
            logger.info { "Loaded contest data" }
            result
        }
        val dump = format(data)
        output.toFile().printWriter().use {
            it.println(dump)
        }
    }
}