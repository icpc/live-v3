import java.net.*

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}


dependencies {
    implementation(projects.cds.core)
    implementation(libs.kotlinx.serialization.json)
    api(libs.kotlin.junit)
}