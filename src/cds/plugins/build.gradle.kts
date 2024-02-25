import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.bcv)
}

apiValidation {
    ignoredProjects.add("plugins")
}

subprojects {
    apply<MavenPublishPlugin>()
}

dependencies {
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.serialization.json)
}