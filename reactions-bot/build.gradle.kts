plugins {
    kotlin("jvm")
    application
    id("icpclive")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("kotlinx.coroutines.FlowPreview")
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

group = "org.icpclive"
version = rootProject.findProperty("build_version")!!

application {
    mainClass.set("org.icpclive.reacbot.BotKt")
}

tasks {
    named<JavaExec>("run") {
        val args = mutableListOf<String>()
        project.properties["live.dev.token"]?.let { args += listOf("-token", it.toString()) }
        project.properties["live.dev.video"]?.let { args += listOf("-video", it.toString()) }
        this.args = args
        this.workingDir(rootDir.resolve("reactions-bot"))
    }
    val fatJar = register<Jar>("fatJar") {
        destinationDir = rootProject.rootDir.resolve("artifacts")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) } + sourcesMain.output)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.jetbrains.exposed", "exposed-core", "0.40.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.40.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.40.1")
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.0.7")
}
