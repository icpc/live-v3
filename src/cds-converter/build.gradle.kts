plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
}

group = "org.icpclive"
version = rootProject.findProperty("build_version")!!
application {
    mainClass.set("org.icpclive.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
    }
}

kotlin {
    sourceSets {
        all {
            languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

tasks {
    jar {
        archiveFileName.set("${project.name}-${project.version}-part.jar")
    }
    named<JavaExec>("run") {
        this.args = buildList {
            add("server")
            project.properties["live.dev.credsFile"]?.let { add("--creds=${it}") }
            project.properties["live.dev.contest"]?.let { add("--config-directory=${it}") }
        }
        this.workingDir(rootDir.resolve("config"))
    }
    task<Copy>("release") {
        from(shadowJar)
        destinationDir = rootProject.rootDir.resolve("artifacts")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.logback)
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
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.cli)
    implementation(projects.cds)
    implementation(projects.common)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.ktor.server.tests)
}
