import org.icpclive.gradle.tasks.GitVersionFilesTask
import org.icpclive.gradle.tasks.PackExamplesTask

plugins {
    id("live.kotlin-conventions")
    id("live.library-conventions")
}

dependencies {
    api(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.server.auth)
    implementation(projects.cds.core)
    implementation(projects.cds.utils)
    api(libs.logback)
}

tasks {

    val gitVersionFiles by registering(GitVersionFilesTask::class) {
        outputDirectory.set(project.layout.buildDirectory.dir("git_version_files"))
    }

    val advancedExamples by registering(PackExamplesTask::class) {
        sourceDirectory.set(rootProject.layout.projectDirectory.dir( provider { "config/_examples/_advanced" }))
        packedDirectory.set(project.layout.buildDirectory.dir("advanced_examples"))
    }
    val visualConfigExamples by registering(PackExamplesTask::class) {
        sourceDirectory.set(rootProject.layout.projectDirectory.dir( provider { "config/_examples/_visual-config" }))
        packedDirectory.set(project.layout.buildDirectory.dir("visual_examples"))
    }

    processResources {
        from(gitVersionFiles) {
            into("org/icpclive/git")
        }
        from(advancedExamples) {
            into("examples/advanced")
        }
        from(visualConfigExamples) {
            into("examples/visual")
        }
    }
}