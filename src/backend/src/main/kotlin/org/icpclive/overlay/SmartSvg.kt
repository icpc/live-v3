package org.icpclive.overlay

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import org.icpclive.admin.getTeams
import org.icpclive.util.Svg
import java.io.File
import java.nio.file.Path

fun Route.configureSvgAtchievementRouting(mediaDirectory: Path) {
    get("{path...}") {
        val relativePath = call.parameters.getAll("path")?.joinToString(File.separator) ?: ""
        val paths = mediaDirectory.resolve(relativePath)

        val substitute = call.request.queryParameters.toMap().mapValues { it.value.first() }.toMutableMap()
        substitute["teamId"]?.let { teamId ->
            getTeams().firstOrNull { it.contestSystemId == teamId }?.let {
                substitute["team.name"] = it.fullName
                substitute["team.shortName"] = it.displayName
                substitute["team.hashTag"] = it.hashTag ?: ""
                substitute["team.info"] = it.customFields["svgInfo"] ?: ""
            }
        }
        call.respondBytes(ContentType.Image.SVG) { Svg.loadAndSubstitute(paths, substitute).toByteArray() }
    }
}
