plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    api(libs.slf4j)
    runtimeOnly(libs.logback)

    testImplementation(libs.kotlin.junit)
}