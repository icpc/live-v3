import org.icpclive.gradle.argProperty
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
        argProperty("live.dev.configDirectory") { "--config-directory=$it" }
        argProperty("live.dev.overlayUrl") { "--overlay=$it" }
        this.workingDir = rootProject.isolated.projectDirectory.asFile
    }

    // Not the best way of doing this, but should work out.
    processResources {
        from(configurations.locatorAdminJsAppResolver) {
            into("locator")
        }
    }
}

dependencies {
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(projects.backendApi)
    implementation(projects.cds.full)
    implementation(projects.serverShared)
    locatorAdminJsApp(projects.frontend)
}
