import java.net.*

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ksp)
    alias(libs.plugins.bcv)
}

tasks {
    dokkaHtml {
        moduleName.set("ICPC-live contest data parser")
        dokkaSourceSets.configureEach {
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
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    implementation(projects.common)
    implementation(projects.clicsApi)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json5)
    implementation(libs.ktor.client.auth)
    api(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.cli)
    ksp(projects.ksp)
    compileOnly(projects.ksp)

    testImplementation(libs.kotlin.junit)
}