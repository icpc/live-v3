plugins {
    id("live.library-conventions")
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.datetime)
    api(libs.kotlinx.serialization.json)
    api(libs.slf4j)
    runtimeOnly(libs.logback)
}