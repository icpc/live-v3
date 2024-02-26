plugins {
    id("live.library-conventions")
}

dependencies {
    api(libs.ktor.client.core)
    implementation(libs.kotlin.reflect)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(projects.cds.core)
    implementation(projects.cds.utils)
}