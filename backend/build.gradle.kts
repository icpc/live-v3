val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jsoup_version: String by project
val datetime_version: String by project
val serialization_version: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.10"
    id("io.ktor.plugin") version "2.1.0"
}

group = "org.icpclive"
version = rootProject.findProperty("version")!!
val jarName = "${rootProject.name}-${project.version}.jar"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")
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


ktor {
    fatJar {
        archiveFileName.set(jarName)
    }
}

val jsList = listOf("overlay", "admin")

tasks {
    named<JavaExec>("run") {
        this.args = listOf("-config=config/application.conf")
    }
    shadowJar { dependsOn("buildJs") }
    compileTestKotlin { dependsOn("buildJs") }
    task("buildJs") {
        for (js in jsList) {
            dependsOn("copyJs${js.capitalize()}")
        }
    }
    for (js in jsList) {
        val dir = rootProject.rootDir.resolve(js)
        register<Copy>("copyJs${js.capitalize()}") {
            dependsOn(":npm_run_build${js.capitalize()}")
            from(dir.resolve("build"))
            destinationDir = project.buildDir.resolve("resources").resolve("main").resolve(js)
        }
    }
    task<Copy>("release") {
        dependsOn("buildFatJar")
        from(project.buildDir.resolve("libs").resolve(jarName))
        destinationDir = rootProject.rootDir.resolve("artifacts")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions {
            kotlinOptions.allWarningsAsErrors = true
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-auto-head-response:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetime_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    implementation("org.jsoup:jsoup:$jsoup_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}
