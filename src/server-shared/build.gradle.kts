plugins {
    id("live.kotlin-conventions")
}

dependencies {
    api(libs.cli)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback)
}