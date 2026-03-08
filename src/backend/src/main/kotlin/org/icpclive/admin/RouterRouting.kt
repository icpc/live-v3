package org.icpclive.admin

import io.ktor.server.routing.*
import org.icpclive.server.*

fun Route.configureLiveRouterRouting() {
    configureLiveRouterRouting(
        listOf(
            MenuItem("Main", "/"),
            MenuItem("Overlay frontend", "/admin"),
            MenuItem("Contest Info", "/admin-configuration")
        ),
        listOf(
            UsefulLink("/admin", "/admin"),
            UsefulLink("/overlay?noStatus", "/overlay?noStatus"),
            UsefulLink("/api/admin/advancedJsonPreview?fields=all", "/api/admin/advancedJsonPreview?fields=all"),
            UsefulLink("https://github.com/icpc/live-v3", "https://github.com/icpc/live-v3")
        )
    )
}
