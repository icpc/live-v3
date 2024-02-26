plugins {
    id("live.kotlin-conventions")
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlinx.serialization.json)
}