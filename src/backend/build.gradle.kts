import org.icpclive.gradle.PackExamplesTask
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

// no idea what does it mean
// copy-pasted from https://docs.gradle.org/8.13/userguide/service_injection.html#execoperations and
// https://docs.gradle.org/8.13/userguide/upgrading_version_8.html#deprecated_project_exec
interface InjectedExecOps {
    @get:Inject val execOps: ExecOperations
}

tasks {
    val gitVersionFiles by registering {
        val branch = layout.buildDirectory.file("git_branch")
        val commit = layout.buildDirectory.file("git_commit")
        val description = layout.buildDirectory.file("git_description")
        outputs.files(branch, commit, description)
        outputs.upToDateWhen { false }
        val injected = project.objects.newInstance<InjectedExecOps>()

        doLast {
            branch.get().asFile.outputStream().use { stream ->
                injected.execOps.exec {
                    executable = "git"
                    args = listOf("rev-parse", "--abbrev-ref", "HEAD")
                    standardOutput = stream
                    isIgnoreExitValue = true
                }
            }
            commit.get().asFile.outputStream().use { stream ->
                injected.execOps.exec {
                    executable = "git"
                    args = listOf("rev-parse", "HEAD")
                    standardOutput = stream
                    isIgnoreExitValue = true
                }
            }
            description.get().asFile.outputStream().use { stream ->
                injected.execOps.exec {
                    executable = "git"
                    args = listOf("describe", "--all", "--always", "--dirty", "--match=origin/*", "--match=v*")
                    standardOutput = stream
                    isIgnoreExitValue = true
                }
            }
        }
    }


    val advancedExamples by registering(PackExamplesTask::class) {
        sourceDirectory.set(rootProject.layout.projectDirectory.dir( provider { "config/_examples/_advanced" }))
        packedDirectory.set(project.layout.buildDirectory.dir("advanced_examples"))
    }

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
        if (project.properties["live.dev.embedFrontend"] == "true") {
            from(project(":frontend").tasks.named("pnpm_run_buildAdmin")) {
                into("admin")
            }
            from(project(":frontend").tasks.named("pnpm_run_buildOverlay")) {
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
    implementation(projects.cds.schemas)
}
