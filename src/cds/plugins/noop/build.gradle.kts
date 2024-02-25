plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    explicitApi()
}

dependencies {
    api(projects.cds.core)
    implementation(projects.cds.utils)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    ksp(projects.ksp)
    compileOnly(projects.ksp)
}