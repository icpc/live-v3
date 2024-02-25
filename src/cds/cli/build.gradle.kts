plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    explicitApi()
}

dependencies {
    api(projects.cds.core)
    api(libs.cli)
    implementation(projects.cds.utils)
}