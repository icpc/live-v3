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

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks {
    runTask {
        this.workingDir(rootDir.resolve("."))
        this.args = listOfNotNull(
            project.properties["live.configDirectory"]?.let { "--config-directory=$it" },
            project.properties["live.overlayUrl"]?.let { "--overlay=$it" },
        )
    }
    processResources {
        into("admin") {
            from(project(":frontend").tasks.named("npm_run_buildLocatorAdmin"))
        }
    }
}

dependencies {
    implementation(projects.common)
    implementation(libs.cli)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
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
    implementation(project(mapOf("path" to ":cds")))

    testImplementation(libs.kotlin.junit)
    testImplementation(libs.ktor.server.tests)
}
