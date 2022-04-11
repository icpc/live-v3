import java.nio.file.Path as NioPath
import kotlin.text.replaceFirstChar

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val gson_version: String by project
val jsoup_version: String by project
val datetime_version: String by project
val serialization_version: String by project

plugins {
    application
    kotlin("jvm") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "org.icpclive"
version = rootProject.findProperty("version")!!
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlin.RequiresOptIn")
        }
    }
}

val jsList = listOf("frontend", "admin")

tasks {
    named("run") {
        (this as JavaExec).args = listOf("-config=config/application.conf")
    }
    shadowJar {
        dependsOn("buildJs")
        manifest {
            attributes("Main-Class" to "io.ktor.server.netty.EngineMain")
        }
        archiveFileName.set("${rootProject.name}-${project.version}.jar")
    }
    task("buildJs") {
        for (js in jsList) {
            dependsOn("copyJs${js.capitalize()}")
        }
    }
    for (js in jsList) {
        val dir = rootProject.rootDir.resolve(js)
        register<Copy>("copyJs${js.capitalize()}") {
            dependsOn(":$js:npm_run_build")
            from(dir.resolve("build"))
            destinationDir = project.buildDir.resolve("resources").resolve("main").resolve(js)
        }
    }
    task<Copy>("release") {
        dependsOn("shadowJar")
        from(project.buildDir.resolve("libs").resolve("${rootProject.name}-${project.version}.jar"))
        destinationDir = rootProject.rootDir
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.google.code.gson:gson:$gson_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    implementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}