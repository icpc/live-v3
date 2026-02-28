import org.icpclive.gradle.tasks.*
import org.gradle.kotlin.dsl.run as runTask

plugins {
    id("live.app-conventions")
}

application {
    mainClass = "org.icpclive.converter.ApplicationKt"
}

tasks.runTask {
    this.args = buildList {
        add("server")
        project.properties["live.dev.credsFile"]?.let { add("--creds=${it}") }
        project.properties["live.dev.contest"]?.let { add("--config-directory=${it}") }
    }
    this.workingDir(rootDir.resolve("config"))
}

dependencies {
    implementation(libs.apache.commons.csv)
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.conditionalHeaders)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.html)
    implementation(libs.ktor.server.auth)
    implementation(projects.cds.full)
    implementation(projects.clicsApi)
    implementation(projects.serverShared)
    adminConfigurationJsApp(projects.frontend)
    adminConverterJsApp(projects.frontend)
    adminRouterJsApp(projects.frontend)
    jsonSchemas(projects.frontend)
}

tasks {
    val gitVersionFiles by registering(GitVersionFilesTask::class) {
        outputDirectory.set(project.layout.buildDirectory.dir("git_version_files"))
    }

    processResources {
        if (project.properties["live.dev.embedFrontend"] == "true") {
            from(configurations.adminConfigurationJsAppResolver) {
                into("admin-configuration")
            }
            from(configurations.adminConverterJsAppResolver) {
                into("admin-converter")
            }
            from(configurations.adminRouterJsAppResolver) {
                into("admin-router")
            }
            from(gitVersionFiles) {
                into("admin-router")
            }
        }
    }
}
