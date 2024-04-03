import org.gradle.kotlin.dsl.run as runTask

plugins {
    id("live.app-conventions")
}


base {
    archivesName = "oracle-tools"
}

application {
    mainClass = "org.icpclive.oracle.ApplicationKt"
}

tasks {
    runTask {
        this.args = listOfNotNull(
            project.properties["live.dev.configDirectory"]?.let { "--config-directory=$it" },
            project.properties["live.dev.overlayUrl"]?.let { "--overlay=$it" },
        )
        this.workingDir = rootDir.resolve(".")
    }

    // Not the best way of doing this, but should work out.
    processResources {
        into("locator") {
            from(project(":frontend").tasks.named("npm_run_buildLocatorAdmin"))
        }
    }
}

dependencies {
    implementation(projects.cds.full)
    implementation(projects.backendApi)
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
