plugins {
    id("live.cds-plugin-conventions")
}

dependencies {
    implementation(projects.cds.utils)
    implementation(projects.cds.ktor)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.contentNegotiation)
}