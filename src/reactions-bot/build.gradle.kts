import org.gradle.kotlin.dsl.run as runTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

application {
    mainClass = "org.icpclive.reacbot.BotKt"
}

tasks.runTask {
    val args = mutableListOf<String>()
    project.properties["live.dev.token"]?.let { args += listOf("-token", it.toString()) }
    project.properties["live.dev.video"]?.let { args += listOf("-video", it.toString()) }
    this.args = args
    this.workingDir(rootDir.resolve("reactions-bot"))
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation(projects.cds)
    implementation(projects.common)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.cli)
    implementation(libs.db.sqlite)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.telegram.bot)
    implementation(libs.retrofit)
}
