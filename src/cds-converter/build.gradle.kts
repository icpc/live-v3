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
    implementation(projects.cds.full)
    implementation(projects.clicsApi)
    implementation(libs.cli)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.autoHeadResponse)
    implementation(libs.ktor.server.callLogging)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.defaultHeaders)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.statusPages)
    implementation(libs.ktor.server.websockets)
    implementation(libs.apache.commons.csv)
    implementation(libs.logback)
}
