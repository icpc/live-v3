package org.icpclive.overlay

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.icpclive.data.DataBus
import org.icpclive.data.currentContestInfo
import org.icpclive.util.loadSVG
import java.io.File
import java.nio.file.Path

fun Route.configureSvgAchievementRouting(mediaDirectory: Path) {
    get("{path...}") {
        val relativePath = call.parameters.getAll("path")?.joinToString(File.separator) ?: ""
        val paths = mediaDirectory.resolve(relativePath)

        val substitute = call.request.queryParameters.toMap().mapValues { it.value.first() }.toMutableMap()
        val contestInfo = DataBus.currentContestInfo()
        call.respondBytes(ContentType.Image.SVG) {
            loadSVG(paths, substitute, contestInfo).toByteArray()
        }
    }
}
