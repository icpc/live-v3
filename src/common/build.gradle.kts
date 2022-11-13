plugins {
    id("icpclive")
}

dependencies {
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.junit)
}
