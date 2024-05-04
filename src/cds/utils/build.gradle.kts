plugins {
    id("live.library-conventions")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    implementation(libs.slf4j)
    implementation(kotlin("reflect"))
}