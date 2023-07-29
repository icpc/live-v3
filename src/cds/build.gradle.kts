import java.net.URI
import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.dokka") version "1.8.20"
    base
}

tasks.dokkaHtml {
    moduleName.set("ICPC-live contest data parser")
    dokkaSourceSets.configureEach {
        // should be moved to another package, as reused by exporter
        perPackageOption {
            matchingRegex.set("org.icpclive.cds.clics.*")
            suppress.set(true)
        }
        perPackageOption {
            matchingRegex.set(".*")
            reportUndocumented.set(true)
            sourceLink {
                localDirectory.set(projectDir)
                remoteUrl.set(URI("https://github.com/icpc/live-v3/tree/main/src/cds").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}

tasks.create<Copy>("doc") {
    from(tasks.dokkaHtml)
    destinationDir = rootProject.rootDir.resolve("docs/cds")
}

dependencies {
    implementation(libs.logback)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.properties)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(projects.common)
    implementation(libs.kotlinx.collections.immutable)
    implementation(kotlin("reflect"))

    testImplementation(libs.kotlin.junit)
    testImplementation("com.approvaltests:approvaltests:18.6.0")
}