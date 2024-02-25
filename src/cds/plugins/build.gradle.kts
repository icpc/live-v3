import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin

plugins {
    alias(libs.plugins.kotlin.jvm)
}

subprojects {
    apply<MavenPublishPlugin>()
}

dependencies {
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.serialization.json)
}