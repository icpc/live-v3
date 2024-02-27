import java.net.*

plugins {
    id("live.kotlin-conventions")
}


dependencies {
    implementation(projects.cds.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(projects.cds.full)
    implementation(libs.kotlin.junit)
}