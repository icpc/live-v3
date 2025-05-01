import org.gradle.kotlin.dsl.run as runTask

plugins {
    id("live.app-conventions")
}

application {
    mainClass = "org.icpclive.ApplicationKt"
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
    implementation(libs.ktor.server.core)
    implementation(libs.kotlinx.html)
    implementation(projects.cds.full)
    implementation(projects.clicsApi)
    implementation(projects.serverShared)
}
