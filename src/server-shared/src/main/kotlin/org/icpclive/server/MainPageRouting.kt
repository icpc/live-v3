package org.icpclive.server

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class GitInfo(val commit: String, val branch: String, val description: String)

@Serializable
data class UsefulLink(val name: String, val url: String)

fun Route.configureMainPageRouting(
    usefulLinks: List<UsefulLink>,
) {
    route("/main-page") {
        get("/git") {
            val commit = javaClass.getResource("/org/icpclive/git/git_commit")?.readText()?.trim() ?: "unknown"
            val branch = javaClass.getResource("/org/icpclive/git/git_branch")?.readText()?.trim() ?: "unknown"
            val description = javaClass.getResource("/org/icpclive/git/git_description")?.readText()?.trim() ?: "unknown"
            call.respond(GitInfo(commit, branch, description))
        }
        get("/links") {
            call.respond(usefulLinks)
        }
    }
}
