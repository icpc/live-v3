package org.icpclive.converter.commands

import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.api.OptimismLevel
import org.icpclive.cds.scoreboard.calculateScoreboard
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.converter.export.clics.ClicsExporter
import org.icpclive.converter.export.clics.ClicsFeedGenerator
import java.io.File

object ClicsArchiveCommand : DumpFileCommand(
    name = "clics-archive",
    help = "Dump CLICS contest archive (zip)",
    defaultFileName = "contest-archive.zip",
    outputHelp = "Path to new zip file"
) {

    val mediaDirectory by option("--media-dir", help = "Directory to store media")
        .path(canBeFile = false, canBeDir = true)
        .defaultLazy("configDirectory/media") { cdsOptions.configDirectory.resolve("media") }

    override fun create(file: File, flow: Flow<ContestUpdate>) {
        runBlocking {
            file.outputStream().use {
                val feed = ClicsFeedGenerator(
                    scope = this,
                    updates = flow.calculateScoreboard(OptimismLevel.NORMAL),
                    isAdmin = true
                )
                ClicsExporter(mediaDirectory).formatArchive(it, feed)
            }
        }
    }
}
