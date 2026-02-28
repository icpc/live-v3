package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class MenuItem(val name: String, val path: String)

@Serializable
data class GitInfo(val commit: String, val branch: String, val description: String)

@Serializable
data class UsefulLink(val name: String, val url: String)

fun Route.configureLiveRouterRouting() {
    get("/menu") {
        call.respond(listOf(
            MenuItem("Main", "/"),
            MenuItem("Overlay frontend", "/admin"),
            MenuItem("Contest Info", "/admin-configuration")
        ))
    }
    get("/git") {
        val commit = javaClass.getResource("/admin-router/git_commit")?.readText()?.trim() ?: "unknown"
        val branch = javaClass.getResource("/admin-router/git_branch")?.readText()?.trim() ?: "unknown"
        val description = javaClass.getResource("/admin-router/git_description")?.readText()?.trim() ?: "unknown"
        call.respond(GitInfo(commit, branch, description))
    }
    get("/links") {
        call.respond(listOf(
            UsefulLink("/admin", "/admin"),
            UsefulLink("/overlay?noStatus", "/overlay?noStatus"),
            UsefulLink("/api/admin/advancedJsonPreview?fields=all", "/api/admin/advancedJsonPreview?fields=all"),
            UsefulLink("https://github.com/icpc/live-v3", "https://github.com/icpc/live-v3")
        ))
    }
}
