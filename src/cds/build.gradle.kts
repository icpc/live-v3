plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    base
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