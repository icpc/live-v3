plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.bcv)
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.ktor.client.core)
    implementation(libs.kotlin.reflect)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(projects.cds.core)
    implementation(projects.common)

    testImplementation(libs.kotlin.junit)
}