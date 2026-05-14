import org.icpclive.gradle.tasks.*
import org.gradle.kotlin.dsl.run as runTask

plugins {
    id("live.app-conventions")
}

base {
    archivesName = rootProject.name
}

application {
    mainClass = "org.icpclive.ApplicationKt"
}

tasks {
    runTask {
        this.args = listOfNotNull(
            "--no-auth",
            project.properties["live.dev.credsFile"]?.let { "--creds=$it" },
            project.properties["live.dev.contest"]?.let { "--config-directory=$it" },
            project.properties["live.dev.analyticsTemplatesFile"]?.let { "--analytics-template=$it" },
        )
        this.workingDir = rootDir.resolve("config")
    }

    processResources {
        if (project.properties["live.dev.embedFrontend"] == "true") {
            from(configurations.adminOverlayJsAppResolver) {
                into("admin-overlay")
            }
            from(configurations.overlayJsAppResolver) {
                into("overlay")
            }
        }
    }
}

dependencies {
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.conditionalHeaders)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.client.cio)
    implementation(libs.logback)
    implementation(projects.backendApi)
    implementation(projects.cds.full)
    implementation(projects.serverShared)
    jsonSchemas(projects.frontend)
    overlayJsApp(projects.frontend)
    adminOverlayJsApp(projects.frontend)
}
