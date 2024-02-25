plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.bcv)
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.coroutines.core)
    implementation(projects.common)
    implementation(projects.clicsApi)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json5)
    implementation(libs.cli)
    ksp(projects.ksp)
    compileOnly(projects.ksp)

    testImplementation(libs.kotlin.junit)
}