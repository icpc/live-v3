package org.icpclive.converter.commands

import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.path
import org.icpclive.cds.cli.CdsCommandLineOptions
import java.nio.file.Path

class ExtendedCdsCommandLineOptions(defaultAutoFinalize: Boolean) : CdsCommandLineOptions() {
    val freeze by option("--freeze", help = "Hide results of frozen submissions in admin data")
        .flag("--no-freeze", default = false, defaultForHelp = "disabled")
    val upsolving by option("--upsolving", help = "Show submissions after contest end in admin data")
        .flag("--no-upsolving", default = false, defaultForHelp = "disabled")
    val autoFinalize by option("--auto-finalize", help = "Automatically finalize the contest")
        .flag("--no-auto-finalize", default = defaultAutoFinalize, defaultForHelp = if (defaultAutoFinalize) "enabled" else "disabled")
    val visualConfigFile: Path by option(
        "--visual-config",
        help = "File with localization of analytics messages"
    ).path(canBeFile = true, canBeDir = false, mustExist = true)
        .defaultLazy("configDirectory/visual-config.json") { configDirectory.resolve("visual-config.json") }
}