package org.icpclive.converter.commands

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.icpclive.cds.cli.CdsCommandLineOptions

class ExtendedCdsCommandLineOptions : CdsCommandLineOptions() {
    val freeze by option("--freeze", help = "Hide results of frozen submissions").flag()
    val upsolving by option("--upsolving", help = "Don't hide submissions after contest end").flag()
    val noAutoFinalize by option("--no-autofinalize", help = "Don't automatically finalize the contest").flag()
}