plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}


dependencies {
    api(projects.cds.core)
    implementation(projects.cds.utils)
    implementation(projects.cds.ktor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.contentNegotiation)
    ksp(projects.ksp)
    compileOnly(projects.ksp)
}