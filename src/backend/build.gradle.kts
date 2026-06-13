import org.icpclive.gradle.*
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
        args("--no-auth")
        argProperty("live.dev.credsFile") { "--creds=$it" }
        argProperty("live.dev.contest") { "--config-directory=$it" }
        argProperty("live.dev.analyticsTemplatesFile") { "--analytics-template=$it" }
        workingDir(rootProject.isolated.projectDirectory.dir("config"))
    }

    processResources {
        from(configurations.adminOverlayJsAppResolver) {
            into("admin-overlay")
        }
        from(configurations.overlayJsAppResolver) {
            into("overlay")
        }
    }
}

val frontendNeeded = booleanGradleProperty("live.dev.embedFrontend")

dependencies {
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.conditionalHeaders)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.logback)
    implementation(projects.backendApi)
    implementation(projects.cds.full)
    implementation(projects.cds.ktor)
    implementation(projects.serverShared)
    jsonSchemas(projects.frontend)
    overlayJsApp(projects.frontend.enabledIf(frontendNeeded))
    adminOverlayJsApp(projects.frontend.enabledIf(frontendNeeded))
}
