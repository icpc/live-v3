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
            project.properties["live.dev.widgetPositionsFile"]?.let { "--widget-positions=$it" },
            project.properties["live.dev.contest"]?.let { "--config-directory=$it" },
            project.properties["live.dev.analyticsTemplatesFile"]?.let { "--analytics-template=$it" },
            project.properties["live.dev.visualConfigFile"]?.let { "--visual-config=$it" },
        )
        this.workingDir = rootDir.resolve("config")
    }

    // Not the best way of doing this, but should work out.
    processResources {
        into("schemas") {
            from(project(":schema-generator").tasks.named("generateAllSchemas"))
        }
        if (project.properties["live.dev.embedFrontend"] == "true") {
            into("admin") {
                from(project(":frontend").tasks.named("pnpm_run_buildAdmin"))
            }
            into("overlay") {
                from(project(":frontend").tasks.named("pnpm_run_buildOverlay"))
            }
        }
    }
}

dependencies {
    implementation(projects.cds.full)
    implementation(projects.backendApi)
    implementation(projects.serverShared)
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.logback)
}
