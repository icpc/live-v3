import org.gradle.kotlin.dsl.run as runTask

plugins {
    id("live.app-conventions")
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
    // Since we're declaring this, we are overriding repositories in the settings.gradle.kts
    mavenCentral()
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
        content {
            // This limits this repo to this group
            includeGroup("io.github.kotlin-telegram-bot.kotlin-telegram-bot")
        }
    }
}

dependencies {
    implementation(projects.cds.full)
    implementation(projects.cds.utils)
    implementation(libs.cli)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.retrofit)
    implementation(libs.telegram.bot)
    runtimeOnly(libs.db.sqlite)
}