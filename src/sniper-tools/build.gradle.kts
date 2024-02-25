import org.gradle.kotlin.dsl.run as runTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

application {
    mainClass = "org.icpclive.sniper.ApplicationKt"
}

tasks {
    runTask {
        this.workingDir(rootDir.resolve("."))
        this.args = listOfNotNull(project.properties["live.overlayUrl"].let { "-P:live.overlayUrl=$it" })
    }
    processResources {
        into("admin") {
            from(project(":frontend").tasks.named("npm_run_buildAdmin"))
        }
    }
}

dependencies {
    implementation(projects.cds.full)
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

    testImplementation(libs.kotlin.junit)
    testImplementation(libs.ktor.server.tests)
}
