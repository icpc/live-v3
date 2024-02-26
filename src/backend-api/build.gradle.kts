plugins {
    id("live.kotlin-conventions")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    api(projects.cds.utils)
    api(projects.cds.core)
}