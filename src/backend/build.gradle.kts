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
    val gitVersionFiles by registering(GitVersionFilesTask::class) {
        outputDirectory.set(project.layout.buildDirectory.dir("git_version_files"))
    }

    val advancedExamples by registering(PackExamplesTask::class) {
        sourceDirectory.set(rootProject.layout.projectDirectory.dir( provider { "config/_examples/_advanced" }))
        packedDirectory.set(project.layout.buildDirectory.dir("advanced_examples"))
    }

    runTask {
        this.args = listOfNotNull(
            "--no-auth",
            project.properties["live.dev.credsFile"]?.let { "--creds=$it" },
            project.properties["live.dev.contest"]?.let { "--config-directory=$it" },
            project.properties["live.dev.analyticsTemplatesFile"]?.let { "--analytics-template=$it" },
            project.properties["live.dev.visualConfigFile"]?.let { "--visual-config=$it" },
        )
        this.workingDir = rootDir.resolve("config")
    }

    processResources {
        if (project.properties["live.dev.embedFrontend"] == "true") {
            from(configurations.adminJsAppResolver) {
                into("admin")
            }
            from(configurations.overlayJsAppResolver) {
                into("overlay")
            }
            from(project(":frontend").projectDir.resolve("main")) {
                into("main")
            }
            from(gitVersionFiles) {
                into("main")
            }
            from(advancedExamples) {
                into("examples/advanced")
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
    implementation(libs.logback)
    implementation(projects.backendApi)
    implementation(projects.cds.full)
    implementation(projects.serverShared)
    jsonSchemas(projects.frontend)
    overlayJsApp(projects.frontend)
    adminJsApp(projects.frontend)
}
