plugins {
    application
    id("icpclive")
    alias(libs.plugins.shadow)
}

version = rootProject.findProperty("build_version")!!

application {
    mainClass.set("org.icpclive.reacbot.BotKt")
}

tasks {
    jar {
        archiveFileName.set("reactions-bot-${project.version}-part.jar")
    }
    shadowJar {
        archiveFileName.set("reactions-bot-${project.version}.jar")
    }
    named<JavaExec>("run") {
        val args = mutableListOf<String>()
        project.properties["live.dev.token"]?.let { args += listOf("-token", it.toString()) }
        project.properties["live.dev.video"]?.let { args += listOf("-video", it.toString()) }
        this.args = args
        this.workingDir(rootDir.resolve("reactions-bot"))
    }
    task<Copy>("release") {
        from(shadowJar)
        destinationDir = rootProject.rootDir.resolve("artifacts")
    }
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
}
